A toy simulation of the OCS-bridge+MCM for developing MCM logic and testing OCS interface.

To run:

git clone https://github.com/tony-johnson/ToyOCSBridge.git
cd ToyOCSBridge
mvn install
mvn "-Dexec.args=-classpath %classpath toyocsbridge.ToyOCSBridge" -Dexec.executable=java org.codehaus.mojo:exec-maven-plugin:1.2.1:exec

Requires: Java >=8 and mvn >=3.0.4
