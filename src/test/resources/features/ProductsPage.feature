Feature: Products Page features

  @regression @productPage01 @sanity @dataFile:config/{env}/data/productsData.json
  Scenario: Verify products page functionality after login
    Given the user is on the login page
    When the user logs in with username "${username}" and password "${password}"
    Then the user should be on the inventory page
    And the user should see at least 1 product on the products page
    And the user sorts products by "${sort}"
    And the user should see product "${product.name}" with price "${product.price}"
    And the user adds product "${product.name}" to the cart
    And the cart badge count should be "1"
