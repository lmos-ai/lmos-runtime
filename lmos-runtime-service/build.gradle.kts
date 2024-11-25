dependencies {

    val springBootVersion: String by rootProject.extra

    implementation(project(":lmos-runtime-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

    testImplementation(testFixtures(project(":lmos-runtime-core")))
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
}