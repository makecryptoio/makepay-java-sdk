import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.plugins.signing.Sign

plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.makecrypto"
version = providers.gradleProperty("makepayVersion").orElse("0.3.0").get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.3")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(11)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

// Central Portal validates Maven repository layout artifacts. Keep the bundle
// focused on Maven artifacts rather than Gradle module metadata.
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "makepay"
            from(components["java"])

            pom {
                name.set("MakePay Java SDK")
                description.set("Official MakePay Java SDK for payment links, donations, bookkeeping, POS, products, Simple Shop, branding, settings, and webhook verification.")
                url.set("https://makepay.io")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("makecrypto")
                        name.set("MakeCrypto")
                        organization.set("MakeCrypto")
                        organizationUrl.set("https://makecrypto.io")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/makecryptoio/makepay-java-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/makecryptoio/makepay-java-sdk.git")
                    url.set("https://github.com/makecryptoio/makepay-java-sdk")
                }
            }
        }
    }

    repositories {
        maven {
            name = "localStaging"
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

val signingKey = providers.environmentVariable("MAVEN_SIGNING_KEY")
    .orElse(providers.gradleProperty("signingKey"))
val signingPassword = providers.environmentVariable("MAVEN_SIGNING_PASSWORD")
    .orElse(providers.gradleProperty("signingPassword"))

signing {
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { signingKey.isPresent }
}

val createCentralBundle by tasks.registering(Zip::class) {
    dependsOn("publishMavenJavaPublicationToLocalStagingRepository")
    archiveFileName.set("makepay-${project.version}-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-bundle"))
    from(layout.buildDirectory.dir("staging-deploy"))
}
