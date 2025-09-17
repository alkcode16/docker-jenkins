pipeline {
    agent any

    environment {
        DB_URL = 'jdbc:postgresql://db:5432/gestion_usuarios_db'
        DB_USER = 'disenio'
        DB_PASS = 'd153N10'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H/2 * * * *') // Revisa cambios cada 2 minutos
    }

    stages {
        
        stage('Kill Previous Instance') {
            steps {
                sh 'pkill -f "java -jar" || true'
            }
        }

        stage('Checkout') {
            steps {
                // git branch: 'prueba', git credentialsId: 'github-jenkins', url: 'https://github.com/Jaim24M/UserRoleBasedTemplate.git'
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
        
        stage('Deploy') {
            steps {
                sh "nohup java -jar target/*.jar --spring.datasource.url=${DB_URL} --spring.datasource.username=${DB_USER} --spring.datasource.password=${DB_PASS} &"
            }
        }
    }

    post {
        success {
            echo '✅ Despliegue exitoso'
        }
        failure {
            echo '❌ Falló el pipeline'
        }
    }
}