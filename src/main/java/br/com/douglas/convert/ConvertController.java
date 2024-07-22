package br.com.douglas.convert;

import io.github.cdimascio.dotenv.Dotenv;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.core.HttpHeaders;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/files")
@ConfigurationProperties(prefix = "convert")
public class ConvertController {

    Dotenv dotenv = Dotenv.load();
    String directory_images = dotenv.get("DIR_IMAGES");

    private final Path convertLocation;
    private final List<String> supportedFormats = Arrays.asList("png", "jpg", "jpeg");

    private String generateUniqueFilename(String originalFilename) {
        // criar nome unico usando UUID
        return originalFilename + "-" + UUID.randomUUID().toString();
    }

    public ConvertController(ConvertProperties convertProperties) {
        this.convertLocation = Paths.get(convertProperties.getUploadDir())
                .toAbsolutePath().normalize();
    }

    private String convertImage(MultipartFile file, String targetFormat, int width, int height) throws IOException {
        // pegar imagem e guardar em buffer
        BufferedImage image = ImageIO.read(file.getInputStream());

        // passando as dimensoes e RGB pra ela
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // criando dimensoes da imagem, tipo o quadro
        Graphics2D g = resizedImage.createGraphics();

        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();

        // Generate a unique filename
        String filenameWithoutExtension = generateUniqueFilename(file.getOriginalFilename());

        // Create the destination file
        File outputFile = new File(convertLocation.toString(), filenameWithoutExtension + "." + targetFormat);

        // Save the resized image
        ImageIO.write(resizedImage, targetFormat, outputFile);

        return filenameWithoutExtension + "." + targetFormat;
    }

    @GetMapping(value = "/")
    public String loadInitialPage() {
        return "index";
    }

    @SuppressWarnings("null")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("width") int width,
            @RequestParam("height") int height,
            @RequestParam(value = "format", required = false, defaultValue = "png") String targetFormat) {

        if (!supportedFormats.contains(targetFormat.toLowerCase())) {
            return ResponseEntity.badRequest().body("Invalid target format. Supported formats: " + supportedFormats);
        }

        try {
            if (!file.getContentType().startsWith("image")) {
                return ResponseEntity.badRequest().body("Only images are allowed.");
            }

            // converter e salvar a imagem no diretorio temporario
            convertImage(file, targetFormat, height, width);

            // Crie o URI de download usando o nome do arquivo convertido
            String convertedFilename = Paths.get(convertLocation.toString(),
                    file.getOriginalFilename() + "." + targetFormat).getFileName().toString();

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/")
                    .path(convertedFilename)
                    .toUriString();

            return ResponseEntity.ok("File uploaded successfully. Download link: " + fileDownloadUri);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
        }
    }

    @PostMapping("/uploads")
    public ResponseEntity<List<String>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("width") int width,
            @RequestParam("height") int height,
            @RequestParam(value = "format", required = false, defaultValue = "png") String targetFormat) {

        if (!supportedFormats.contains(targetFormat.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(List.of("Invalid target format. Supported formats: " + supportedFormats));
        }

        List<String> downloadUris = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                convertImage(file, targetFormat, height, width);
                String convertedFilename = Paths.get(convertLocation.toString(),
                        file.getOriginalFilename() + "." + targetFormat).getFileName().toString();

                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/files/")
                        .path(convertedFilename)
                        .toUriString();

                downloadUris.add(fileDownloadUri);
            } catch (IOException ex) {
                System.out.println(
                        "Error converting file: " + file.getOriginalFilename() + ", Reason: " + ex.getMessage());
            }

        }

        if (downloadUris.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(List.of("No files uploaded successfully."));
        }

        return ResponseEntity.ok(downloadUris);

    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request)
            throws IOException {

        Path filePath = convertLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() throws IOException {
        List<String> fileNames = Files.list(convertLocation)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileNames);
    }

    @GetMapping("/downloads")
    public ResponseEntity<Resource> downloadZippedFiles(HttpServletRequest request) throws IOException {
        List<Path> fileNames = Files.list(convertLocation)
                .map(Path::getFileName)
                .collect(Collectors.toList());

        if (fileNames.isEmpty()) {
            return ResponseEntity.ok().body(new ByteArrayResource("No files found to download".getBytes()));
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        for (Path fileName : fileNames) {
            Path filePath = convertLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            ZipEntry zipEntry = new ZipEntry(fileName.toString());
            zipOutputStream.putNextEntry(zipEntry);

            try (InputStream inputStream = resource.getInputStream()) {
                IOUtils.copy(inputStream, zipOutputStream);
            } catch (IOException e) {
                // Tratar a exceção adequadamente, aqui estou apenas imprimindo a mensagem
                System.err.println("Error copying file '" + fileName + "' to ZIP: " + e.getMessage());
            } finally {
                zipOutputStream.closeEntry(); // Feche a entrada ZIP após a cópia do arquivo
            }
        }

        zipOutputStream.close(); // Feche o stream de saída após a conclusão do loop
        byte[] zipBytes = outputStream.toByteArray();
        String contentDisposition = "attachment; filename=\"all_files.zip\"";

        // Pegando o caminho do arquivo
        Path uploadsDir = Path.of(System.getProperty("user.dir")).resolveSibling(directory_images);
        try {
            Files.walkFileTree(uploadsDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error deleting files from uploads directory: " + e.getMessage());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(new ByteArrayResource(zipBytes));
    }

}
