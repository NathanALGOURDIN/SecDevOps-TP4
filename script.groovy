pipeline{
    agent any
    environment{
        //variable 
        CONTAINER_NAME = "trufflehog"
        URL_REPOSITORY = "https://github.com/MarvinPedron/ISEN-M1-SecDevOps"
        OUTPUT_JSON_FILE = "formatedFile.json"
        IMAGE_NAME = "trufflesecurity/trufflehog"
    }
    stages{
        stage("Initialize"){
            steps{
                echo "Le workspace : ${WORKSPACE}"
                echo "Le nom du job : ${JOB_NAME}"
                echo "L'utilisateur : ${USER}"
                sh 'uname'
                echo "suppression des fichiers dans les dossiers ${WORKSPACE}"
                cleanWs()
            }
        }
        stage("Deploy"){
            steps{
                script{
                    int commandReturn = sh(script: "docker inspect ${env.CONTAINER_NAME}" , returnStatus: true) // if container didn't exist error code 1 else 0 
                    if(commandReturn == 1){
                        //container doesn't exist
                        sh "docker run --name ${env.CONTAINER_NAME} trufflesecurity/trufflehog:latest github --repo ${env.URL_REPOSITORY} --only-verified --json | jq > ${env.OUTPUT_JSON_FILE} "
                        archiveArtifacts artifacts: "${OUTPUT_JSON_FILE}", onlyIfSuccessful: true
                    }else{
                        //container exist
                        sh "docker start -a ${env.CONTAINER_NAME} | jq > ${env.OUTPUT_JSON_FILE}"
                        //get creation date of container and 7 days before to day
                        def dateContainerCreated = sh(script:"docker container inspect ${env.CONTAINER_NAME} | jq '.[].Created' | grep -o -e '[0-9]*-[0-9]*-[0-9][0-9]'", returnStdout: true)
                        def dateOneWeekAgo = sh(script:"date --date '7 days ago' +%Y-%m-%d", returnStdout:true)
                        //echo "date 1 : ${dateContainerCreated} et date 2 : ${dateOneWeekAgo}"
                        archiveArtifacts artifacts: "${OUTPUT_JSON_FILE}", onlyIfSuccessful: true
                        if( dateContainerCreated == dateOneWeekAgo){
                            //update container for the next build 
                            sh 'docker image rm -f ${env.IMAGE_NAME}'
                            sh 'docker container rm ${env.CONTAINER_NAME}'
                        }
                    }
                }
            
            }
        }
    }
}
