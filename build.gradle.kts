allprojects {
    group = "pl.jms.auth"
    version = "2.0.0"
}

subprojects {
    apply(plugin = "java")
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
}
