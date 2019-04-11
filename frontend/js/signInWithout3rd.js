$(function () {

    if (GLOBAL_CONFIG.platform === 'Beijing') {
        $('#center-logo').attr('src', "images/EnSaaS-BJ.svg");
    } else if (GLOBAL_CONFIG.platform === 'HongKong') {
        $('#center-logo').attr('src', "images/EnSaaS-HK.svg");
    } else if (GLOBAL_CONFIG.platform === 'Ali') {
        $('#center-logo').attr('src', "images/EnSaaS-Alibaba.svg");
    } else {
        $('#center-logo').attr('src', "images/EnSaaS-Advantech.png");
    }

    $.ajax({
        url: getSSOUri() + '/params',
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    }).done(function (webParams) {
        function validateEmail(email) {
            var mailRegex = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            return mailRegex.test(email);
        }

        function inputValidate() {
            var email = $('#login-username').val();
            var password = $('#login-pwd').val();
            if (email === '') {
                notifyForMay('warning', langs[getLang()]['emptyUsernameMessage']);
                return false;
            }
            if (password === '') {
                notifyForMay('warning', langs[getLang()]['emptyPwdMessage']);
                return false;
            }
            if (validateEmail(email) === false) {
                notifyForMay('warning', langs[getLang()]['invalidUsernameMessage']);
                return false;
            }
            return true;
        }

        $('#login-pwd').keypress(function (e) {
            
            var key = e.which;
            // key code 13 = enter
            if (key === 13 && $('#login-btn').css('pointer-events') === 'auto') {
                $('#login-btn').click();
            }
        });

        $('#rememberMe').keypress(function (e) {
            var key = e.which;
            if (key === 13 && $('#login-btn').css('pointer-events') === 'auto') {
                $('#login-btn').click();
            }
        });

        var params = decodeURIComponent(window.location.search.substring(1) + window.location.hash);
        var redirectUri = '', responseType = '', code = '', clientId = '', state = '', failMessage = '';
        params.split('&').forEach(function (param) {
            var redirecUriIndex = param.indexOf('redirectUri=');
            if (redirecUriIndex > -1) {
                redirectUri = param.substring(redirecUriIndex + 12);
            }
            var responseTypeIndex = param.indexOf('responseType=');
            if (responseTypeIndex > -1) {
                responseType = param.substring(responseTypeIndex + 13);
            }
            var codeIndex = param.indexOf('code=');
            if (codeIndex > -1) {
                code = param.substring(codeIndex + 5);
            }
            var clientIdIndex = param.indexOf('clientId=');
            if (clientIdIndex > -1) {f
                clientId = param.substring(clientIdIndex + 9);
            }
            var stateIndex = param.indexOf('state=');
            if (stateIndex > -1) {
                state = param.substring(stateIndex + 6);
            }
            var failMessageIndex = param.indexOf('failMessage=');
            if (failMessageIndex > -1) {
                failMessage = param.substring(failMessageIndex + 12);
                if (failMessage.substring(failMessage.length - 1, failMessage.length) !== '.') {
                    failMessage += '.';
                }
            }
        });
        if (responseType === 'code') {
            redirectUri += '?code=' + code + '&state=' + state;
        }

        if (failMessage !== '') {
            failMessage === 'The token provided is invalid.' ?
                notifyForMay('warning', langs[getLang()]['mpFailMessage'] + langs[getLang()]['signInAgain']) :
                notifyForMay('warning', failMessage + langs[getLang()]['signInAgain']);
        } else if (sessionStorage.getItem('failMessage') !== null) {
            notifyForMay('warning', sessionStorage.getItem('failMessage') + langs[getLang()]['signInAgain']);
            sessionStorage.removeItem('failMessage');
        }

        redirectUri = redirectUri !== '' ? redirectUri : GLOBAL_CONFIG.webHostUrl + '/redirectPage.html';
        console.log('If sign in successfully, you will be redirecting to: \n' + redirectUri);

        function initPasswordChangeModal(modalDesc, cancelBtnText, forwardPage) {
            $('#modalDesc').html(modalDesc);
            if (cancelBtnText !== '') {
                $('#cancelBtn').text(cancelBtnText);
                $('#cancelBtn').click(function () {
                    window.location.href = redirectUri;
                });
            } else {
                $('#cancelBtn').remove();
            }
            $('#changePwdBtn').click(function () {
                if (forwardPage === 'unlock.html') {
                    sessionStorage.setItem('username', $('#login-username').val());
                    sessionStorage.setItem('redirectUri', redirectUri);
                }
                window.location.href = GLOBAL_CONFIG.webHostUrl + '/' + forwardPage;
            });
            $('#passwordChangeModal').modal({
                backdrop: 'static',
                keyboard: false
            });
        }

        whoAmI();
        function whoAmI() {
            $.ajax({
                url: GLOBAL_CONFIG.apiHostUrl + '/users/me',
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            }).done(function (user) {
                window.location.href = redirectUri;
            }).fail(function (jqXHR, textStatus, errorThrown) {
                if (typeof (Cookies.get('dXNlcm5hbWU')) !== 'undefined') {
                    $('#login-username').val(window.atob(Cookies.get('dXNlcm5hbWU')));
                    $('#rememberMe').prop('checked', true);
                }

                $('#login-btn').click(function () {
                    if (!inputValidate())
                        return;

                    $('#login-btn').find('span').prepend('<i class="fa fa-spinner fa-spin"></i>&nbsp;');
                    $('#login-btn').css('pointer-events', 'none');

                    var auth = {
                        'username': $('#login-username').val(),
                        'password': encodeURIComponent($('#login-pwd').val()),
                        'redirectUri': redirectUri
                    }

                    var apiPath = (responseType === 'code') ? '/auth?responseType=' + responseType + '&code=' + code + '&clientId=' + clientId
                        + '&redirectUri=' + redirectUri.substring(0, redirectUri.indexOf('?')) : '/auth?redirectUri=' + redirectUri;
                    $.ajax({
                        url: GLOBAL_CONFIG.apiHostUrl + apiPath,
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: JSON.stringify(auth)
                    }).done(function (generalResponse) {
                        console.log(JSON.stringify(generalResponse));
                        if ($('#rememberMe').prop('checked')) {
                            Cookies.set('dXNlcm5hbWU', window.btoa($('#login-username').val()), { expires: 30 });
                        } else {
                            Cookies.remove('dXNlcm5hbWU');
                        }

                        if (generalResponse.status === 'first_time') {
                            initPasswordChangeModal(langs[getLang()]['firstTimeSignedInMessage'], '', 'unlock.html');
                        } else if (generalResponse.status === 'password_expires') {
                            initPasswordChangeModal(langs[getLang()]['passwordExpiresForcedMessage'], '', 'unlock.html');
                        } else if (generalResponse.status === 'passed') {
                            $.ajax({
                                url: GLOBAL_CONFIG.apiHostUrl + '/users/me',
                                method: 'GET',
                                headers: {
                                    'Content-Type': 'application/json'
                                }
                            }).done(function (user) {
                                // first time
                                if (user.totalSignedInTimes === 1) {
                                    initPasswordChangeModal(langs[getLang()]['firstTimeSignedInMessage'], langs[getLang()]['skip'], 'changePassword.html');
                                } else {
                                    if (webParams.passwordExpires.enabled) {
                                        if (Date.now() - Date.parse(user.lastModifiedPwdTime) > webParams.passwordExpires.age) {
                                            initPasswordChangeModal(langs[getLang()]['passwordExpiresMessage'], langs[getLang()]['remindMeNextTime'], 'changePassword.html');
                                        } else {
                                            window.location.href = auth.redirectUri;
                                        }
                                    } else {
                                        window.location.href = auth.redirectUri;
                                    }
                                }
                            }).fail(function (jqXHR, textStatus, errorThrown) {
                                var failMessage = jqXHR.responseJSON.message;
                                notifyForMay('warning', failMessage);
                            });
                        }
                    }).fail(function (jqXHR, textStatus, errorThrown) {
                        $('#login-btn').html('<span class="noselect">' + langs[getLang()]['signIn'] + '</span>');
                        $('#login-btn').css('pointer-events', 'auto');
                        var failMessage = jqXHR.responseJSON.message;
                        notifyForMay('warning', failMessage);
                    });

                });
            });
        }
    }).fail(function (jqXHR, textStatus, errorThrown) {
        var failMessage = jqXHR.responseJSON.message;
        notifyForMay('warning', failMessage);
    });

});