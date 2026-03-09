my github tests say this: Run ./local.sh                                                                                                                                                                                             
The JAVA_HOME environment variable is not defined correctly,                                                                                                                                                                                  
this environment variable is needed to run this program.                                                                                                                                                                                      
Error: Process completed with exit code 1.

● The runner doesn't have Java set up. Since the workflow just runs ./local.sh, we need to install Java inside the script itself.

● Write(local.sh)
⎿  Added 8 lines
1  #!/bin/bash
2  set -e
3  
4 +# Install Java 21 if not available                                                                                                                                                                                                 
5 +if ! command -v java &> /dev/null; then                                                                                                                                                                                            
6 +    sudo apt-get update -qq                                                                                                                                                                                                        
7 +    sudo apt-get install -y openjdk-21-jdk                                                                                                                                                                                         
8 +fi                                                                                                                                                                                                                                 
9 +                                                                                                                                                                                                                                   
10 +export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))                                                                                                                                                                
11 +                                                                                                                                                                                                                                   
12  cd gcas-api
13  ./mvnw clean verify