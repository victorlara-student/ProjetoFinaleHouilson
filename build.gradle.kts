plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.projetofinal"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Driver JDBC do PostgreSQL, resolvido automaticamente pelo Gradle
    // (não precisa mais de nenhum .jar manual em pasta nenhuma do seu PC).
    implementation("org.postgresql:postgresql:42.7.4")
}

application {
    // Ponto de entrada: a função main() está em src/main/kotlin/Main.kt
    // (sem pacote), então o nome da classe gerada pelo compilador é "MainKt".
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}
