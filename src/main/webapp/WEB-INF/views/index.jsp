<%@ page language="java" contentType="text/html charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html lang="pt-br">

    <head>
        <meta charset="UTF-8">
        <title>Upload de Imagens</title>
        <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
            integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">
    </head>

    <body>
        <div class="jumbotron jumbotron-fluid">
            <div class="container">
                <h1 class="display-4">Conversor e Redimensionador de Imagens</h1>
            </div>
        </div>
        <div class="container mt-4">
            <div class="row justify-content-center">
                <div class="col-md-6">
                    <form method="post" enctype="multipart/form-data">
                        <div class="form-group">
                            <input type="file" id="files" name="files" class="form-control-file" multiple>
                        </div>

                        <div class="form-group">
                            <label for="width">Largura</label>
                            <input type="text" class="form-control" id="width" name="width" placeholder="Largura">

                            <label for="height">Altura</label>
                            <input type="text" class="form-control" id="height" name="height" placeholder="Altura">
                        </div>

                        <div class="form-group">
                            <label for="format">Formato</label>
                            <select class="custom-select mr-sm-1" name="format" id="format">
                                <option value="png">png</option>
                                <option value="jpg">jpg</option>
                                <option value="jpeg">jpeg</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <button type="button" class="btn btn-primary" onclick="uploadFiles()">Enviar</button>
                            <button type="button" class="btn btn-primary" id="downloadButton" disabled
                                onclick="downloadAll()">Baixar</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <script>
            function uploadFiles() {
                const xhr = new XMLHttpRequest()
                const formData = new FormData()
                const filesInput = document.getElementById("files").files
                const weightValue = document.getElementById("width").value
                const heightValue = document.getElementById("height").value
                const formatValue = document.getElementById("format").value

                if (filesInput.length === 0) {
                    alert('Selecione pelo menos um arquivo para enviar.')
                    return
                }

                for (const fileInput of filesInput) {
                    formData.append('files', fileInput)
                }

                formData.append('width', weightValue)
                formData.append('height', heightValue)
                formData.append('format', formatValue)


                xhr.open("POST", "http://10.25.1.219:8010/api/files/uploads")
                xhr.onload = function () {
                    if (xhr.status === 200) {
                        alert('Arquivo(s) enviado(s) com sucesso!')
                        document.getElementById("downloadButton").disabled = false
                    } else {
                        alert('Erro ao enviar arquivo(s)')
                    }
                }

                xhr.send(formData)
            }

            function downloadAll() {
                const xhr = new XMLHttpRequest()

                xhr.open("GET", "http://10.25.1.219:8010/api/files/downloads", true)
                xhr.responseType = "blob"

                xhr.onload = function (e) {
                    if (this.status === 200) {
                        var blob = new Blob([xhr.response], { type: 'application/zip' })
                        var downloadUrl = URL.createObjectURL(blob)
                        var a = document.createElement("a")
                        a.style.display = 'none'
                        a.href = downloadUrl
                        a.download = 'files.zip'
                        document.body.appendChild(a)
                        a.click()
                        document.body.removeChild(a)
                        URL.revokeObjectURL(downloadUrl)
                        // recarregar a pafina depois 2 segundos
                        setTimeout(function () {
                            window.location.reload();
                        }, 2000);

                    } else {
                        console.error("Erro ao baixar o arquivo:", xhr.statusText)
                    }
                }

                xhr.send()
            }
        </script>

        <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"
            integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy"
            crossorigin="anonymous"></script>
    </body>

    </html>