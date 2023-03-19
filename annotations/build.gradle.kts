plugins {
    id("local.java-library")
    id("local.maven-publish")
}

base.archivesName.set("auto-delegate")

nullaway {
    annotatedPackages.add("net.ltgt.auto.delegate")
}
