plugins {
    java
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.google.inject:guice:5.1.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation(project(":core"))
    implementation("org.yaml:snakeyaml:2.2")
}

tasks.jar {
    archiveBaseName.set("UltimateBungeeAuth-Velocity")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}
