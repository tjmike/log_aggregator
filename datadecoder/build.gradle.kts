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
    compile("javax.xml.bind:jaxb-api:2.2.6:")
    compile("org.apache.camel:camel-core:3.0.0-RC2");
    compile("org.apache.camel:camel-spring-boot-starter:3.0.0-RC2");
    compile("org.apache.camel:camel-file-watch-starter:3.0.0-RC2");


    implementation("org.springframework.boot:spring-boot-starter")
    implementation(files("../proto/build/libs/proto.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")

}

application {
    mainClassName = "tjmike.datastax.datadecoder.DataDecoder"
}

val test by tasks.getting(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
}


configure<SourceSetContainer> {
    named("main") {
        java.srcDir("build/generated/source/proto/main/java")
    }
}

