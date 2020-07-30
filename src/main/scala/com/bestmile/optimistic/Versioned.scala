package com.bestmile.optimistic

import java.util.UUID

import endpoints4s.algebra

/**
 * A versioned resource of type `A`
 * @param value   Resource value
 * @param version Resource version
 * @tparam A      Type of the resource
 */
case class Versioned[A](value: A, version: Version)

/**
 * A version (which is an arbitrary string value, here)
 */
case class Version(value: String)

object Version {

  def random(): Version = Version(UUID.randomUUID().toString)

}

/**
 * Enriches endpoints4s algebra with operations for handling versioned resources.
 */
trait VersionedEndpoints extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas {

  // Since we get raw headers’ values, we need to manually decode and encode them
  // For now, we only support non-weak etag values, see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
  private def decodeETag(rawETag: String): Version = Version(rawETag.stripPrefix("\"").stripSuffix("\""))
  private def encodeETag(version: Version): String = "\"" + version.value + "\""

  /**
   * An `ETag` response header carrying a resource `Version`
   */
  val eTagHeader: ResponseHeaders[Version] =
    responseHeader("ETag", docs = Some("Resource version")).xmap(decodeETag)(encodeETag)
  /**
   * An `If-Match` request header carrying a resource `Version`
   */
  val ifMatchHeader: RequestHeaders[Version] =
    requestHeader("If-Match", docs = Some("Last known resource version")).xmap(decodeETag)(encodeETag)

  /**
   * An OK (200) HTTP response carrying a JSON entity representing the resource,
   * and an `ETag` header containing the resource version.
   *
   * @param schema JSON schema for the resource
   * @tparam A     Type of the resource
   */
  def versionedResponse[A](implicit schema: JsonSchema[A]): Response[Versioned[A]] =
    ok(
      jsonResponse[A],
      headers = eTagHeader,
      docs = Some("The resource content in the response entity, and the resource version in the ETag header.")
    ).xmap {
      case (value, version) => Versioned(value, version)
    } {
      versioned => (versioned.value, versioned.version)
    }

  /**
   * A Precondition Failed (412) HTTP response. This response is returned to clients who
   * tried to update a resource that has already been updated since the last time the
   * client read the resource.
   */
  val preconditionFailedResponse: Response[Unit] =
    response(
      PreconditionFailed,
      emptyResponse,
      docs = Some("The resource could not be updated because a concurrent update has already been applied.")
    )

  /**
   * An HTTP response that can be either Precondition Failed, or OK (see [[preconditionFailedResponse]]
   * and [[versionedResponse]]).
   *
   * - Servers map `None` into a Precondition Failed response, and `Some` into an OK response,
   * - Conversely, clients map a Precondition Failed response into `None`, and an OK response
   *   into `Some`.
   *
   * @param schema JSON schema for the resource
   * @tparam A     Type of the resource
   */
  def preconditionFailedOrVersionedResponse[A](implicit schema: JsonSchema[A]): Response[Option[Versioned[A]]] =
    preconditionFailedResponse.orElse(versionedResponse[A])
      .xmap(_.toOption)(_.toRight(()))

}
