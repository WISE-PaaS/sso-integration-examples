$(function () {

    // prevent adding hash sign on URL
    $('a[href="#"]').click(function (event) {
        event.preventDefault();
    });

    setTitle();
    function setTitle() {
        $.ajax({
            url: getSSOUri() + '/params',
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        }).done(function (webParams) {
            $(document).prop('title', webParams.title + ' SSO');
        });
    }

    $(window).resize(function () {
        refreshSelect2s();
    });

    // call this when the select element is first displayed on the UI
    window.initSelect2 = function ($select) {
        $select.select2();
    }

    window.refreshSelect2s = function () {
        $('select').each(function (i, v) {
            if ($(this).prop('id') !== 'pageSelector' && $(this).is(':visible')) {
                initSelect2($(this));
            }
        });
    }

    // only select elements that need to hide themselves need to be destroyed before reinitialization
    window.destroySelect2 = function ($select) {
        if ($select.hasClass('select2-hidden-accessible')) {
            $select.select2('destroy');
        }
    }

    window.getLang = function () {
        var params = decodeURIComponent(window.location.search.substring(1) + window.location.hash);
        var lang = '';
        params.split('&').forEach(function (param) {
            var langIndex = param.indexOf('lang');
            if (langIndex > -1) {
                lang = param.substring(lang + 5);
            }
        });
        var host = window.location.host;
        // remove old version of lang setting
        if (host.indexOf('localhost') === -1) {
            Cookies.remove('lang');
        }
        var cookieDomain = host.indexOf('localhost') > -1 ? 'localhost' : host.substring(host.indexOf('.'), host.length);
        // parameter first, cookie secondary
        if (lang === 'zh-TW') {
            Cookies.set('lang', lang, { domain: cookieDomain });
        } else if (lang === 'zh-CN') {
            Cookies.set('lang', lang, { domain: cookieDomain });
        } else if (lang === 'en-US') {
            Cookies.set('lang', 'en-US', { domain: cookieDomain });
        } else {
            if ((Cookies.get('lang') !== 'zh-TW' && Cookies.get('lang') !== 'zh-CN') || typeof Cookies.get('lang') === 'undefined') {
                Cookies.set('lang', 'en-US', { domain: cookieDomain });
            }
        }
        return Cookies.get('lang');
    };

    setLang();
    function setLang() {
        var lang = getLang();
        console.log('current lang: ' + lang);
        $('.lang').each(function (index, element) {
            $(this).html(langs[lang][$(this).attr('text-key')]);
            $(this).prop('placeholder', langs[lang][$(this).attr('placeholder-key')]);
            $(this).prop('title', langs[lang][$(this).attr('title-key')]);
        });
    }

    window.notifyForMay = function notify(type, message) {
        var icon = 'fa fa-envelope';
        if (message.indexOf(':') === 5) {
            message = message.substring(7, message.length);
        }
        if (type === 'success') {
            icon = 'fa fa-check-circle';
        } else if (type === 'warning') {
            icon = 'fa fa-warning';
        }

        $.notify({
            icon: icon,
            message: message
        }, {
                type: type,
                placement: {
                    from: 'bottom', align: 'center'
                },
                z_index: 2000,
                delay: 3000,
                animate: {
                    enter: 'animated slideInUp',
                    exit: 'animated slideOutDown'
                },
                template: '<div data-notify="container" class="col-xs-10 col-sm-6 col-md-4 col-lg-3 alert alert-{0}" role="alert" style="opacity: 1;">' +
                    '<button type="button" aria-hidden="true" class="close" data-notify="dismiss">×</button>' +
                    '<span data-notify="icon"></span> ' +
                    '<span data-notify="title">{1}</span> ' +
                    '<span data-notify="message" style="word-wrap: break-word;">{2}</span>' +
                    '<div class="progress" data-notify="progressbar">' +
                    '<div class="progress-bar progress-bar-{0}" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div>' +
                    '</div>' +
                    '<a href="{3}" target="{4}" data-notify="url"></a>' +
                    '</div>'
            });
    }

    window.notify = function notify(type, message) {
        var icon = 'fa fa-envelope';
        if (message.indexOf(':') === 5) {
            message = message.substring(7, message.length);
        }
        if (type === 'success') {
            icon = 'fa fa-check-circle';
        } else if (type === 'warning') {
            icon = 'fa fa-warning';
        }

        $.notify({
            icon: icon,
            message: message
        }, {
                type: type,
                placement: {
                    from: 'bottom', align: 'right'
                },
                z_index: 2000,
                delay: 3000,
                animate: {
                    enter: 'animated fadeIn',
                    exit: 'animated fadeOut'
                }
            });
    }

    window.handleAjaxDoneForMay = function (mainMessage, redirectingPage) {
        if (getLang() === 'en-US') {
            mainMessage += ' ';
        }
        mainMessage += langs[getLang()]['redirectingMessage'];
        setTimeout(function () {
            window.location.href = redirectingPage;
        }, 5000);
        notifyForMay('success', mainMessage);
    }

    window.handleAjaxDone = function (mainMessage, action, redirectingPage) {
        if (getLang() === 'en-US') {
            mainMessage += ' ';
        }
        if (action === 'back') {
            mainMessage += langs[getLang()]['redirectingMessage'];
            setTimeout(function () {
                window.history.back();
            }, 5000);
        } else if (action === 'redirect') {
            mainMessage += langs[getLang()]['redirectingMessage'];
            setTimeout(function () {
                window.location.href = redirectingPage;
            }, 5000);
        } else if (action === 'refresh') {
            mainMessage += langs[getLang()]['refreshingMessage'];
            setTimeout(function () {
                window.location.reload();
            }, 5000);
        }
        notify('success', mainMessage);
    }

    setValidatorErrorMessage();
    function setValidatorErrorMessage() {
        var lang = getLang();
        $('input').each(function (index, element) {
            if (element.required) {
                $(this).attr('data-required-error', langs[lang]['data-required-error']);
            }
            if (element.pattern !== '') {
                if ($(this).attr('pattern-level') === 'basic') {
                    $(this).attr('data-pattern-error', langs[lang]['data-pattern-error-basic']);
                } else {
                    $(this).attr('data-pattern-error', langs[lang]['data-pattern-error']);
                }
            }
            if (element.minlength !== '') {
                $(this).attr('data-minlength-error', langs[lang]['data-minlength-error']);
            }
            if (element['data-match-error'] !== '') {
                $(this).attr('data-match-error', langs[lang]['data-match-error']);
            }
            if (element['data-error'] !== '') {
                if (element.type === 'email') {
                    $(this).attr('data-error', langs[lang]['data-error-email']);
                } else if (element.type === 'url') {
                    $(this).attr('data-error', langs[lang]['data-error-url']);
                } else {
                    $(this).attr('data-error', langs[lang]['data-error']);
                }
            }
        });
    }

    function handleForbidden(failMessage) {
        // error code 40348 appears when user is disabled during signed in
        if (failMessage.substring(0, 5) === '40348') {
            sessionStorage.setItem('failMessage', failMessage.substring(7));
            window.location.href = 'signOut.html?&redirectUri=' + GLOBAL_CONFIG.webHostUrl + '/signIn.html?redirectUri='
                + GLOBAL_CONFIG.webHostUrl + '/portals.html';
        }
        $('.btn').prop('disabled', true);
        $('.btn-default').prop('disabled', false);
        failMessage.indexOf('Access is denied.') > -1 ?
            notify('warning', langs[getLang()]['accessIsDenied'] + ' ' + langs[getLang()]['redirectingMessage']) :
            notify('warning', failMessage + ' ' + langs[getLang()]['redirectingMessage']);
        setTimeout(function () {
            window.location.href = 'getUserProfile.html';
        }, 5000);
    }

    window.handleAjaxFail = function (jqXHR, textStatus, errorThrown) {
        var failMessage = jqXHR.responseJSON.message;
        if (jqXHR.responseJSON.status === 401) {
            sessionStorage.setItem('failMessage', failMessage);
            window.top.location.href = GLOBAL_CONFIG.webHostUrl + '/signIn.html?redirectUri=' + GLOBAL_CONFIG.webHostUrl + '/redirectPage.html';
        } else if (jqXHR.responseJSON.status === 403) {
            handleForbidden(failMessage);
        } else {
            notify('warning', failMessage);
        }
    }

    window.postToMp = function () {
        window.parent.postMessage('403', getSSOUri().replace('portal-sso', 'portal-management'));
    }

});