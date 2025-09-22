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