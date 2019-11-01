/*
 *
 */
import org.gradle.api.tasks.testing.logging.TestLogEvent
// import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
  repositories {
    mavenCentral()
  }
}

plugins {
    java
    id("org.springframework.boot") version "2.2.0.RELEASE"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    compile("com.google.protobuf:protobuf-java:3.10.0")
    compile("com.google.api.grpc:proto-google-common-protos:1.17.0")
    compile("javax.xml.bind:jaxb-api:2.2.6:")

    compile("org.apache.camel:camel-core:3.0.0-RC2");
    compile("org.apache.camel:camel-spring-boot-starter:3.0.0-RC2");
    compile("org.apache.camel:camel-file-watch-starter:3.0.0-RC2");

    implementation("org.springframework.boot:spring-boot-starter")
    implementation(files("../proto/build/libs/proto.jar"))

    testCompile("org.springframework.boot:spring-boot-starter-test:2.2.0.RELEASE")
    testCompile("org.mock-server:mockserver-netty:5.6.1");
    testImplementation("ch.qos.logback:logback-classic:1.1.7") {
      isForce = true
    }
    testImplementation("ch.qos.logback:logback-core:1.1.7") {
      isForce = true
    }
//    testCompile("ch.qos.logback:logback-classic:1.2.3");
}

tasks {
    test {
        testLogging.showExceptions = true
        testLogging.displayGranularity  = 0
        testLogging.events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED) // , TestLogEvent.STARTED)
    }
}


application {
    mainClassName = "tjmike.logaggregator.datapump.DataPump"
}

