> [!WARNING]
> Matilda is a prototype. Please report any issues and be mindful when using it in production.

# Matilda
Matilda provides sandboxing capabilities at runtime for the JVM. It is a lightweight alternative to the soon to be deprecated Java Security Manager. You can granuallary block modules from executing System.exit(), System.exec() and Network connections like Socket.open.
Matilda 

## Installation
Currently Matilda only supports JDK Version 23 or Higher as it heavily uses the[Class File API](https://docs.oracle.com/en/java/javase/23/vm/class-file-api.html). To use Matilda download the MatildaAgent.jar and the MatildaBootstrap.jar from the repository.


# Usage
Matilda can be used via the CLI or by configuring the projects build file accordingly. 

## CLI Quickstart
Enable preview features when using JDK 23 in order to be able to use the Class-File API
```bash
--enable-preview
```
Hook the MatildaAgent into your application
```bash
-javaagent:/path/to/matilda-agent-1.0-SNAPSHOT.jar
```

Add the MatildaAcceControl to the bootpath. This is needed due to the class loading hirachy. Classes manipulated by the MatildaAgent reference to the MatildaAccessControl.
```bash
-Dmatilda.bootstrap.jar=/path/to/matilda-bootstrap-1.0-SNAPSHOT.jar"
```
Note that Matilda works with a whitelisting approach. With enabling the MatildaAgent, all calls to the above mentioned methods will be blocked by default.

For gradle examples refer to the [Log4Shell example](https://github.com/khaleesicodes/Matilda/blob/main/Log4Shell_Test/build.gradle)


## Configuration
Matilda comes with a module-based whitelisting approach, permission can be set per module and are enforced accordingly. If your projects does not use modules consider to change it, it is not only needed to use Matilda but also recommenede by the [Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html)

Configuration can also be done via the CLI or build file following the naming scheme:
```bash
-Dmatilda.runtime.exit.allow=module <insert module name here>
-Dmatilda.system.exec.allow=module <insert module name here>
-Dmatilda.network.connect.allow=module <insert module name here>
```


# POC Log4Shell
Matilda is a tool that can be used to to reduce the impact of supply chain attacks by setting granular permissions for each module. As an example a proof of concept for Log4Shell can be found in the [Log4Shell_POC](https://github.com/khaleesicodes/Matilda/tree/main/Log4Shell_Test).

In order to test just clone this repository and run it with gradle

To run it with the MatildaAgent use the following gradle command, the build should be successful and return " Matilda has successfully blocked log4shell"
```bash
gradle run
```
To test the exploitable version run the following gradle command. The build should fail and return "LogForShell was not blocked by matilda"
```bash
gradle runNoAgent
```

## Configuration Example - tomcat
In order to run apache tomcat with Matilda just export CATALINA_OPTS with the following configuration.

```bash
export CATALINA_OPTS="--enable-preview -javaagent:./path/matilda-agent-1.0-SNAPSHOT.jar -Dmatilda.bootstrap.jar=./path/matilda-bootstrap-1.0-SNAPSHOT.jar"
```
