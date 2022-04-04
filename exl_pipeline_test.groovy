#!groovy

def JSON_URL="http://3.64.207.71:5555/exl_hands_on_lab/feeds.json"

def WHEATHER_API_KEY="ba12adb54ba864479838324a5909608f"

pipeline {
    agent any
    parameters {
        string(defaultValue: "25.0", description: 'Maximum Temperature Allowed in Celsius, please use integers or float numbers', name: 'MAX_TEMP')
        choice(choices: ['list', 'set'], description: 'Operation type', name: 'OPT')
        string(defaultValue: "", description: "Operation arguments, for 'list': specify only <field name>, for 'set' specify <id> <field name> <field desired value>", name: 'ARGS')
    }
    stages {
        stage('Pre-processing') {
            steps {
                script {
                    
                    STATUS_CODE = sh (
                        script: 'curl -s -o /dev/null -w "%{http_code}" http://3.64.207.71:5555/exl_hands_on_lab/feeds.json',
                        returnStdout: true
                    ).trim()
                    
                    echo "Status code to JSON file: ${STATUS_CODE}"
                        if ("200"!="${STATUS_CODE}"){
                            error 'JSON not accessible - Stopping the pipeline.'
                        } else {
                            println 'JSON accessible - Continuing.'
                        }
                    
                    WHEATHER_API_RESP = sh (
                        script: "curl 'https://api.openweathermap.org/data/2.5/onecall?lat=31.771959&lon=35.217018&exclude=minutely,hourly,daily,alerts&units=metric&appid=${WHEATHER_API_KEY}'",
                        returnStdout: true
                    ).trim()

                    def jsonObj = readJSON text: "$WHEATHER_API_RESP"
                    println "current temperature in Israel is: ${jsonObj.current.temp}"

                    CURRENT_TEMP_FLOAT = Float.valueOf("${jsonObj.current.temp}")
                    MAX_TEMP_FLOAT = Float.valueOf("${MAX_TEMP}")
                    
                    println "Checking if current temperature is higher than allowed"
                        if (Float.compare(MAX_TEMP_FLOAT, CURRENT_TEMP_FLOAT) == 0){
                            println 'Current temperature is equal as Maximum Temperature Allowed - Continuing.'
                        } else if (Float.compare(MAX_TEMP_FLOAT, CURRENT_TEMP_FLOAT) < 0) {
                            error 'Maximum Temperature Allowed exceeded! - Stopping the pipeline.'
                        } else {
                            println 'Current temperature is below Maximum Temperature Allowed - Continuing.'
                        }
                }    
            }
        }
        stage('Processing') {
            steps {
                git branch: 'master',
                    credentialsId: '37e4f5ce-c088-49b5-88a5-67c49badc606',
                    url: 'https://github.com/jorzel-87/exl_feed_ctl.git'

                sh "./exl_feed_ctl.py ${OPT} ${ARGS}"
            }
        }
        stage('Post-processing') {
            steps {
                script {
                println "Checking if feed.json file is present"
                        sh (
                            script: """
                            if test -f ./feed.json; then
                            echo 'File feed.json exists. Continuing.' && ./exl_feed_convert.py
                            else
                            echo "json file doesn't exist - you used 'list' option, which is expected behaviour OR 'set' parameter didn't work correctly."
                            fi
                            """
                        )
                    
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}

