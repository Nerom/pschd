dependencies {
    compile("com.esotericsoftware:kryo:4.0.1")
    compile("com.alipay.sofa:bolt:1.5.6")
    // https://mvnrepository.com/artifact/com.alipay.sofa/hessian
    compile("com.alipay.sofa:hessian:4.0.3")
    compile("com.lmax:disruptor:3.4.2")
    compile("com.google.guava:guava:28.0-jre")
    testImplementation("junit:junit")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("ch.qos.logback:logback-core")
}