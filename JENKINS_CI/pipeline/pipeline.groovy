pipeline {
    agent any

    environment {
        DB_URL = 'jdbc:postgresql://db:5432/gestion_usuarios_db'
        DB_USER = 'disenio'
        DB_PASS = 'd153N10'
        
        // Define el nombre de la imagen Docker que vas a construir
        DOCKER_IMAGE = "user-role-based-app:${env.BUILD_ID}"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'prueba', credentialsId: 'github-jenkins', url: 'https://github.com/Jaim24M/UserRoleBasedTemplate.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw test'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    // Detiene y elimina el contenedor si ya existe
                    sh "docker stop user-app || true"
                    sh "docker rm user-app || true"

                    // Inicia un nuevo contenedor con la imagen recién construida
                    sh "docker run -d --name user-app -p 8080:8080 --env-file <(printf 'SPRING_DATASOURCE_URL=${DB_URL}\\nSPRING_DATASOURCE_USERNAME=${DB_USER}\\nSPRING_DATASOURCE_PASSWORD=${DB_PASS}') ${DOCKER_IMAGE}"
                }
            }
        }
    }

    post {
        success {
            echo '✅ Despliegue exitoso en contenedor Docker'
        }
        failure {
            echo '❌ Falló el pipeline'
        }
    }
}

//MICROSERVICIOS

def changedDirs = sh(script: "git diff --name-only HEAD~1 HEAD | cut -d/ -f1 | sort -u", returnStdout: true).trim().split("\n")

pipeline {
    agent any
    stages {
        stage('Detectar cambios') {
            steps {
                script {
                    echo "Microservicios modificados: ${changedDirs}"
                }
            }
        }

        stage('Compilar y desplegar') {
            steps {
                script {
                    if ('auth-service' in changedDirs) {
                        buildAndDeploy('auth-service')
                    }
                    if ('cita-service' in changedDirs) {
                        buildAndDeploy('cita-service')
                    }
                    // Repetir para otros servicios
                }
            }
        }
    }
}

def buildAndDeploy(service) {
    sh "cd ${service} && mvn clean package -DskipTests"
    sh "docker build -t aleksei/${service} ${service}"
    sh "docker push aleksei/${service}"
    sh "docker-compose up -d ${service}"
}

//CONTENERIZAR UN PROYECTO DE SPRING BOOT

pipeline {
    agent any
    environment {
        DB_URL = 'jdbc:postgresql://db:5432/gestion_usuarios_db'
        DB_USER = 'disenio'
        DB_PASS = 'd153N10'
        
        // Define el nombre de la imagen Docker que vas a construir
        DOCKER_IMAGE = "user-role-based-app:${env.BUILD_ID}"
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'prueba', credentialsId: 'github-jenkins', url: 'https://github.com/Jaim24M/UserRoleBasedTemplate.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw test'
            }
        }
        
        // stage('Deploy') {
        //     steps {
        //         script {
        //             // Detiene y elimina el contenedor si ya existe
        //             sh "docker stop user-app || true"
        //             sh "docker rm user-app || true"

        //             // Inicia un nuevo contenedor con la imagen recién construida
        //             sh "docker run -d --name user-app -p 8080:8080 --env-file <(printf 'SPRING_DATASOURCE_URL=${DB_URL}\\nSPRING_DATASOURCE_USERNAME=${DB_USER}\\nSPRING_DATASOURCE_PASSWORD=${DB_PASS}') ${DOCKER_IMAGE}"
        //         }
        //     }
        // }
        
        stage('Build Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
            }
            sh "docker build -t ${DOCKER_IMAGE} ."

                }
            }
        }
        
    }

    post {
        success {
            echo '✅ Despliegue exitoso en contenedor Docker'
        }
        failure {
            echo '❌ Falló el pipeline'
        }
    }
}


///ANGULAR
pipeline {
    agent any

    environment {
        NODE_VERSION = '18' // Ajusta según tu versión
    }

    tools {
        nodejs "${NODE_VERSION}"
    }

    stages {
        stage('Checkout') {
            steps {
                git credentialsId: 'github-jenkins', url: 'https://github.com/Jaim24M/Proyect-Gys-Issste.git'
            }
        }

        stage('Instalar dependencias') {
            steps {
                sh 'npm install'
            }
        }

        stage('Compilar Angular') {
            steps {
                sh 'npm run build -- --configuration=production'
            }
        }

        stage('Archivar artefactos') {
            steps {
                archiveArtifacts artifacts: 'dist/**', fingerprint: true
            }
        }

        stage('Publicar') {  
            when {
                branch 'main'
            }
            steps {
                sshagent(['credenciales-servidor']) {
                    sh '''
                        # Elimina archivos anteriores en el servidor
                        # ssh sigys@192.167.165.242 "rm -rf /var/www/html/*"
                        ssh sigys@192.167.165.242 "rm -rf /home/sigys/Documentos/SIGYS/jenkins/*"

                        # Copia los archivos compilados al servidor
                        # scp -r dist/* sigys@192.167.165.242:/var/www/html/
                        scp -r dist/* sigys@192.167.165.242:/home/sigys/Documentos/SIGYS/jenkins/
                    '''
                }
            }
        }

    }

    post {
        success {
            echo 'Compilación exitosa.'
        }
        failure {
            echo 'La compilación falló.'
        }
    }
}

// sudo chown -R usuario:www-data /var/www/html
// Esto da permisos de lectura y ejecución a todos, pero solo escritura al propietario:
// sudo chmod -R 755 /var/www/html