plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mindrot:jbcrypt:0.4")
}
