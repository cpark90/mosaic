load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")



def mosaic_rules():
    """Loads common dependencies needed to compile the protobuf library."""
    if not native.existing_rule("rules_java"):
        http_archive(
        name = "rules_java",
        sha256 = "220b87d8cfabd22d1c6d8e3cdb4249abd4c93dcc152e0667db061fb1b957ee68",
        url = "https://github.com/bazelbuild/rules_java/releases/download/0.1.1/rules_java-0.1.1.tar.gz",
        )

    if not native.existing_rule("io_bazel_rules_docker"):
        http_archive(
            name = "io_bazel_rules_docker",
            sha256 = "62b79a11bae076a8acd97749a06e5b8448c793ec5a730aea925e68c6f46dcaff",
            strip_prefix = "rules_docker-73964b225c38052bbfb1ea556bfb49d2bd2edc77",
            urls = ["https://github.com/uhthomas/rules_docker/archive/73964b225c38052bbfb1ea556bfb49d2bd2edc77.tar.gz"],
        )
  
    if not native.existing_rule("rules_jvm_external"):
        RULES_JVM_EXTERNAL_TAG = "2.2"
        RULES_JVM_EXTERNAL_SHA = "f1203ce04e232ab6fdd81897cf0ff76f2c04c0741424d192f28e65ae752ce2d6"

        http_archive(
            name = "rules_jvm_external",
            strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
            sha256 = RULES_JVM_EXTERNAL_SHA,
            url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
        )

def mosaic_proto_rules():
    """Loads common dependencies needed to compile the protobuf library."""
    if not native.existing_rule("mosaic_proto"):
        git_repository(
            name = "mosaic_proto",
            remote = "https://github.com/cpark90/mosaic_proto",
            tag = "v0.01",
        )