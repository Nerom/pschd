package wang.nerom.pschd.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.pool.KryoPool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Serializable

object SerializationUtil {
    private val kryoPool = KryoPool.Builder { Kryo() }.build()

    /**
     * 序列化
     *
     * @param bodyObj
     * @param <T>
     * @return
     * @throws SerializeException
    </T> */
    fun <T : Serializable> serialize(bodyObj: T?): ByteArray {
        if (bodyObj == null) return ByteArray(0)

        ByteArrayOutputStream().use { bos ->
            Output(bos).use { output ->
                return kryoPool.run { kryo ->
                    kryo.writeObject(output, bodyObj)
                    output.flush()
                    bos.toByteArray()
                }
            }
        }
    }

    /**
     * 反序列化
     *
     * @param bodyBytes
     * @return
     * @throws SerializeException
     */
    fun <T : Serializable> deserialize(bodyBytes: ByteArray?, clazz: Class<T>): T? {
        if (bodyBytes == null) return null
        ByteArrayInputStream(bodyBytes).use { bis ->
            Input(bis).use { input ->
                return kryoPool.run { kryo ->
                    kryo.readObject(
                        input,
                        clazz
                    )
                }
            }
        }
    }
}