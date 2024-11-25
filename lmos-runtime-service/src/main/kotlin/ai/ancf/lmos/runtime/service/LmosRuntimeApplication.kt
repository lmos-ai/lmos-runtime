package ai.ancf.lmos.runtime.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
open class LmosRuntimeApplication

fun main(args: Array<String>) {
    runApplication<LmosRuntimeApplication>(*args)
}
