/*
Note: this is only needed if you're lazy loading the plugin script itself (intlTelInput.js). If not then just use the utilsScript option.
Load the utils.js script (included in the lib directory) to enable formatting/validation etc. See Utilities Script for more information.
*/
$.fn.intlTelInput.loadUtils("../libs/intl-tel-input/js/utils.js");

var telInput = $('#login-phone');
var loginForm = $('#login-form input');
var loginFormMessage = $('#login-message');
var loginFormOriginalMessage = $('#login-message').text();
var phoneInvalidMessage = 'Phone format is incorrect';

telInput.intlTelInput({
    initialCountry: "auto",
    geoIpLookup: function(callback) {
        $.get('https://ipinfo.io', function() {}, "jsonp").always(function(resp) {
            var countryCode = (resp && resp.country) ? resp.country : "";
            callback(countryCode);
        });
    },
    dropdownContainer: 'body'
});

var reset = function() {
    loginForm.removeClass("phone-invalid");
    loginFormMessage.html(loginFormOriginalMessage).css('color', '#342e2d');
};

// on blur: validate
telInput.blur(function() {
    reset();
    if ($.trim(telInput.val())) {
        if (telInput.intlTelInput("isValidNumber")) {
            loginForm.removeClass("phone-invalid");
            loginFormMessage.html(loginFormOriginalMessage).css('color', '#342e2d');
        } else {
            loginForm.addClass("phone-invalid");
            loginFormMessage.html(phoneInvalidMessage).css('color', '#E6757B');
        }
    }
});
// on keyup / change flag: reset
telInput.on("keyup change", reset);