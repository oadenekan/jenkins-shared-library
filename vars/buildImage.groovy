#!/user/bin/env groovy

def call () {
    echo "building the docker image..."
    withCredentials([script.usernamePassword(credentialsId: 'docker-hub-repo', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
        sh "docker build -t olusolaayeni/demo-app:3.0 ."
        sh "echo '${PASS}' | docker login -u '${USER}' --password-stdin"
        sh "docker push olusolaayeni/demo-app:3.0"
    }
}