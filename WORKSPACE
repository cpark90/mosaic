#WORKSPACE
workspace(name = "mosaic")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

load("//bazel_scripts:mosaic_rules.bzl", "mosaic_rules")
mosaic_rules()

# protocol buffer

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies")

rules_proto_dependencies()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()


# rules_docker is not strictly necessary, but interesting if you want to create
# and/or push docker containers for the examples. The docker executable is
# needed only if you want to run an image using Bazel.

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load(
    "@io_bazel_rules_docker//java:image.bzl",
    _java_image_repos = "repositories",
)

_java_image_repos()

load("//bazel_scripts:mosaic_java_deps.bzl", "mosaic_java_deps")
mosaic_java_deps()