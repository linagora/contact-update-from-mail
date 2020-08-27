require('../helper.service.js');
require('../shell/contact-shell-display-builder.service.js');
require('../../app.constant.js');

(function(angular) {
  'use strict';

  angular.module('linagora.esn.contact')
    .directive('contactDisplay', contactDisplay);

  function contactDisplay(
    $state,
    ContactsHelper,
    ContactShellDisplayBuilder,
    CONTACT_AVATAR_SIZE,
    $http,
    session
  ) {
    return {
      restrict: 'E',
      scope: {
        contact: '='
      },
      template: require("./contact-display.pug"),
      link: function($scope) {

        $scope.displayShell = ContactShellDisplayBuilder.build($scope.contact);
        $scope.avatarSize = CONTACT_AVATAR_SIZE.bigger;
        ContactsHelper.fillScopeContactData($scope, $scope.contact);

        $scope.hasContactInformation = function() {
          return ($scope.contact.emails && $scope.contact.emails.length > 0) ||
                 ($scope.contact.tel && $scope.contact.tel.length > 0) ||
                 ($scope.contact.addresses && $scope.contact.addresses.length > 0) ||
                 ($scope.contact.social && $scope.contact.social.length > 0) ||
                 ($scope.contact.urls && $scope.contact.urls.length > 0);
        };

        $scope.hasProfileInformation = function() {
          return !!($scope.contact.firstName ||
                    $scope.contact.lastName ||
                    $scope.contact.nickname ||
                    $scope.contact.birthday);
        };

        $scope.getPrediction = function() {
          
          //alert(JSON.stringify(session.user.preferredEmail));
          //alert(session.user.preferredEmail);
          $http({
            method: 'GET',
            url: 'http://localhost:8080/api/contacts/predictionList/' + session.user.preferredEmail + '/' + $scope.contact.emails[0].value
          }).then(function successCallback(response) {
              if (!angular.isUndefined(response.data.data[0])) {
                
                if (!angular.isUndefined(response.data.data[0].phone)) {
                  $scope.prediction = "Predictions :"
                  $scope.predictedPhone = response.data.data[0].phone;
                }
                //alert(JSON.stringify(response.data.data[0].phone));
                //alert(response.data.data[0]);
              }
            }, function errorCallback(response) {

            });
            //alert('clack')
        };

        $scope.getPrediction();

        $scope.shouldDisplayWork = function() {
          return !!($scope.contact.orgName || $scope.contact.orgRole);
        };

        $scope.openAddressbook = function() {
          $state.go('contact.addressbooks', {
            bookId: $scope.contact.addressbook.bookId,
            bookName: $scope.contact.addressbook.bookName
          });
        };
      }
    };
  }
})(angular);
