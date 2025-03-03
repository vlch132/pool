plugins {
    kotlin("jvm") version "2.1.0"
}

group = "ru.vlch"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.h2database:h2:latest.release")

    testImplementation("org.junit.jupiter:junit-jupiter:latest.release")
    testImplementation("com.h2database:h2:latest.release")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}