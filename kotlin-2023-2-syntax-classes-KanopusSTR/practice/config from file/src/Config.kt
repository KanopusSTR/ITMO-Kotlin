import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Config(config: String) {

    private var valueKeys: MutableMap<String, String> = mutableMapOf()

    operator fun provideDelegate(nothing: Nothing?, property: KProperty<*>): ReadOnlyProperty<Nothing?, String> {
        if (property.name !in valueKeys) throw IllegalArgumentException("${property.name} not in config keys")
        return ReadOnlyProperty { _, _ -> valueKeys.getValue(property.name) }
    }

    init {
        getResource(config).use { it ->
            it?.reader()?.forEachLine {
                val pair = it.split('=').map { s -> s.trim() }
                valueKeys[pair.first()] = pair.last()
            } ?: throw IllegalArgumentException("Error while reading config file")
        }
    }
}

