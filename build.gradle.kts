plugins {
    id("java")
}

group = "me.lorenzo.presence"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation ("com.github.libraries-p:services:3585e64ee0")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation(platform("org.mongodb:mongodb-driver-bom:5.6.4"))
    implementation("org.mongodb:mongodb-driver-sync")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}