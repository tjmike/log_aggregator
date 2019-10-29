/*
 *
 */
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.10")
  }
}

plugins {
    java
    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("com.google.protobuf") version "0.8.10"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

apply(plugin = "io.spring.dependency-management")

repositories {
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    compile("com.google.protobuf:protobuf-java:3.10.0")
    compile("com.google.api.grpc:proto-google-common-protos:1.17.0")
    implementation(files("../proto/build/libs/proto.jar"))

     testImplementation("org.springframework.boot:spring-boot-starter-test")
}


application {
    mainClassName = "tjmike.datastax.logServer.LogServer"
}


tasks {
    test {                                  
        testLogging.showExceptions = true
        testLogging.displayGranularity  = 0
	testLogging.events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STARTED)
    }
}


// Unpack the fat jar so we can build the docker image in multiple steps
// task<Copy>("unpack") {
    // val bootJar = tasks.getByName<BootJar>("bootJar")
    // dependsOn(bootJar)
    // from(zipTree(bootJar.outputs.files.singleFile))
    // into("build/dependency")
// }





