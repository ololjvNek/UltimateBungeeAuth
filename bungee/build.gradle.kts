plugins {
    java
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("io.github.waterfallmc:waterfall-api:1.21-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation(project(":core"))
    implementation("org.yaml:snakeyaml:2.2")
}

tasks.jar {
    archiveBaseName.set("UltimateBungeeAuth-Bungee")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("bungee.yml") {
        expand(props)
    }
}
