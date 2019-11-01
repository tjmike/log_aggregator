/*
 * 
 */
import org.gradle.api.tasks.testing.logging.TestLogEvent
//import org.gradle.api.tasks.testing.logging.TestExceptionFormat

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
    compile( project(":proto") )

    implementation("org.springframework.boot:spring-boot-starter")
    implementation(files("proto/build/libs/proto.jar"))

    testCompile("org.springframework.boot:spring-boot-starter-test:2.2.0.RELEASE")

}
tasks {
    test {
        testLogging.showExceptions = true
        testLogging.displayGranularity  = 0
        testLogging.events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED) // , TestLogEvent.STARTED)
    }
}


application {
    mainClassName = "tjmike.logaggregator.agent.LogAgent"
}


configure<SourceSetContainer> {
    named("main") {
        java.srcDir("proto/build/generated/source/proto/main/java")
    }
}

