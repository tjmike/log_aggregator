/*
 * 
 */

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
    implementation(files("../proto/build/libs/proto.jar"))


}

application {
    mainClassName = "tjmike.datastax.agent.LogAgent"
}


configure<SourceSetContainer> {
    named("main") {
        java.srcDir("build/generated/source/proto/main/java")
    }
}

