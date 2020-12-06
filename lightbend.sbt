resolvers in ThisBuild += "lightbend-commercial-mvn" at
  "https://repo.lightbend.com/pass/03ClxKn8SdpGb4yBuecEMDVMu6_cXvmvDuDcvl8SXFDBRkFw/commercial-releases"
resolvers in ThisBuild += Resolver.url("lightbend-commercial-ivy",
  url("https://repo.lightbend.com/pass/03ClxKn8SdpGb4yBuecEMDVMu6_cXvmvDuDcvl8SXFDBRkFw/commercial-releases"))(Resolver.ivyStylePatterns)