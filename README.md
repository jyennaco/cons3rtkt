# cons3rtkt

A JVM integration for cons3rt assets.

## CLI Commands

```
# Deployment

## Environment Vars

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep env
ASSET_DIR:
CONS3RT_CREATED_USER:
CONS3RT_ROLE_NAME:
DEPLOYMENT_HOME: /Users/yennaco/Downloads/sample_deployments/Deployment15233
DEPLOYMENT_RUN_HOME: /Users/yennaco/Downloads/sample_deployments/Deployment15233/run/389423

## Network

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep net --name cons3rt-net
172.16.10.5

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep net --host scanner
172.16.14.5

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep net --host scanner --name cons3rt-net
172.16.10.5

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep net --host scanner --name cons3rt-net --info
scanner,cons3rt,interface0,cons3rt-net,172.16.10.5,13.77.230.34

## Classic Properties

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type properties --prop cons3rt.user
helpfuljoe

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type properties --prop REMOVE_INBOX_FILES
1

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type properties --prop cons3rt.fap.deployment.machine.scanner.interface1.internalIp
172.16.14.5

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type properties --list
SOURCE_BUCKET
REMOVE_INBOX_FILES
TARGET_BUCKET

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type properties --regex cons3rt.user.*
cons3rt.user
cons3rt.user.email

## JSON Properties

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type json --list
SOURCE_BUCKET
REMOVE_INBOX_FILES
TARGET_BUCKET

java -jar  build/libs/cons3rtkt-1.0-SNAPSHOT.jar dep props --type json --prop SOURCE_BUCKET
inbox94120/inbox

```

## Running on Linux

```
# The full set of commands as shown above, one example:

cons3rtkt dep env
```

## Running on Windows

```
# The full set of commands as shown above, one example:

C:\Cons3rtKt\cons3rtkt\bin\cons3rtkt.bat dep env
```

