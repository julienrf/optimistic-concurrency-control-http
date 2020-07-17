package com.bestmile.optimistic

trait ResourcesApi extends VersionedEndpoints {

  /** The URL of our resource: “/resource” */
  private val resourceUrl: Url[Unit] = path / "resource"

  val read: Endpoint[Unit, Versioned[Resource]] =
    endpoint(
      get(resourceUrl),
      versionedResponse[Resource],
      docs = EndpointDocs().withSummary(Some("Read the current state of the resource"))
    )

  val update: Endpoint[(Resource, Version), Option[Versioned[Resource]]] =
    endpoint(
      put(resourceUrl, jsonRequest[Resource], headers = ifMatchHeader, docs = Some("New resource content")),
      preconditionFailedOrVersionedResponse[Resource],
      docs = EndpointDocs()
        .withSummary(Some("Update the resource"))
        .withDescription(Some(
          """|The request contains the new resource content in its entity,
             |and the last known resource version in its `If-Match` header.
             |
             |If the current version of the resource has changed since the last time the client
             |has read the resource, a `Precondition Failed` error response is returned.
             |Otherwise, an `OK` response is returned.""".stripMargin))
    )

  /**
   * The JSON schema for our [[Resource]] type
   */
  implicit lazy val resourceSchema: JsonSchema[Resource] =
    field[String]("content").xmap(Resource)(_.content)

}

/**
 * A dummy entity, just for the example
 */
case class Resource(content: String)
