@wiremock #@disabled
Feature: WireMock Command
  Creating WireMock stubs with WireMockCommand and overriding URL properties with WireMockConfig

  Scenario Outline: Unstubbed path returns 404
    When a HEAD request is sent to "<path>"
    Then a HttpClientErrorException should be thrown
    And the exception message should contain "404 Not Found"

    Examples:
      | path                |
      | /non/existing/url/1 |

  Scenario Outline: Request to a path different from the stubbed one returns 404
    Given a WireMock stub for HEAD "<path>" returning 400
    When a HEAD request is sent to "<path>notfound"
    Then a HttpClientErrorException should be thrown
    And the exception message should contain "404 Not Found"

    Examples:
      | path                |
      | /non/existing/url/2 |

  Scenario Outline: Request with an HTTP method not matching the stub is not matched
    Given a WireMock stub for DELETE "<path>" returning 200
    When a GET request is sent to "<path>"
    Then a HttpClientErrorException should be thrown
    And the exception message should contain "Request was not matched"

    Examples:
      | path                |
      | /non/existing/url/3 |

  Scenario Outline: Stubbed GET request returns status, headers and body
    Given a WireMock stub for GET "<path>" returning 202 with content type "image/png" and ETag "test-etag" and JSON body containing key "responseFromGet" with value "<path>"
    When a GET request is sent to "<path>"
    Then no exception should be thrown
    And the response status should be 202
    And the response content type should be "image/png"
    And the response ETag should be "test-etag"
    And the response body should contain key "responseFromGet" with value "<path>"

    Examples:
      | path                |
      | /non/existing/url/4 |
      | /some/other/url4    |

  Scenario Outline: Stubbed POST request returns body loaded from classpath
    Given a WireMock stub for POST "<path>" returning 201 with body from classpath "wiremock/self-test/response1.json"
    When a POST request is sent to "<path>"
    Then no exception should be thrown
    And the response status should be 201
    And the response content type should be "application/json"
    And the response body should contain key "testKey" with value "test value"

    Examples:
      | path                |
      | /non/existing/url/5 |
      | /some/other/url5    |

  Scenario Outline: Unauthorized stub throws HttpClientErrorException
    Given a WireMock stub for PUT "<path>" returning 401
    When a PUT request is sent to "<path>"
    Then a HttpClientErrorException should be thrown
    And the exception message should contain "Unauthorized"

    Examples:
      | path                |
      | /non/existing/url/6 |

  Scenario Outline: Repeated request and response builder sections overwrite HTTP status
    Given a WireMock stub built with repeated request and response sections for PUT "<path>" with final status 204
    When a PUT request is sent to "<path>"
    Then no exception should be thrown
    And the response status should be 204

    Examples:
      | path                |
      | /non/existing/url/7 |

  Scenario Outline: Stub rebuilt with toBuilder overwrites HTTP status for the same URL
    Given a WireMock stub for PUT "<path>" returning 401
    And the stub is rebuilt with toBuilder returning 308
    When a PUT request is sent to "<path>"
    Then no exception should be thrown
    And the response status should be 308

    Examples:
      | path                |
      | /non/existing/url/8 |
