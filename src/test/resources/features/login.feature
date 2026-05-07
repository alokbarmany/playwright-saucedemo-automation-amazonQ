Feature: Login Page features

  @regression @loginPage01 @smoke @dataFile:config/{env}/data/loginData.json
  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "${username}" and password "${password}"
    Then the user should be on the inventory page
    Then user should see the dashboard header text "Products"
    Then user print address information from "${address}" section of the data file
