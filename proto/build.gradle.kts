/*

 */
// https://github.com/google/protobuf-gradle-plugin

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.10")
  }
}


plugins {
    // Apply the java plugin to add support for Java
    java

    // id("com.google.protobuf") version "0.8.10"

}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}


dependencies {
    compile("com.google.protobuf:protobuf-java:3.10.0")
    compile("com.google.api.grpc:proto-google-common-protos:1.17.0")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")

}

//application {
//     Define the main class for the application
//    mainClassName = "tjmike.datastax.agent.App"
//}

val test by tasks.getting(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
}


configure<SourceSetContainer> {
    named("main") {
        java.srcDir("build/generated/source/proto/main/java")
    }
}

