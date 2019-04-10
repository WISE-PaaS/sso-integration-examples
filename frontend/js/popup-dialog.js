function showPopupDialog(type, title, message) {
    var icons = {
        'warning': 'fa fa-exclamation',
        'success': 'fa fa-check-circle-o',
        'info': 'fa fa-exclamation',
        'danger': 'fa fa-times-circle-o'
    };

    $.notify({
        // options
        icon: icons[type],
        title: title,
        message: message
    },{
        // settings
        element: 'body',
        type: type,
        position: null,
        // 請不要改變pop-up dialog的位置!
        placement: {
            from: "bottom",
            align: "center"
        },
        allow_dismiss: true,
        newest_on_top: true,
        offset: 20,
        spacing: 10,
        z_index: 1031,
        delay: 1000,
        timer: 2000,
        animate: {
            enter: 'animated fadeInUp',
            exit: 'animated fadeOutDown'
        },
        icon_type: 'class',
        template: '<div data-notify="container" class="col-xs-11 col-sm-3 alert alert-{0}" role="alert">' +
            '<button type="button" aria-hidden="true" class="close" data-notify="dismiss">×</button>' +
            '<h4 data-notify="title"><span data-notify="icon"></span> {1}</h4> ' +
            '<span data-notify="message">{2}</span>' +
            '<div class="progress" data-notify="progressbar">' +
                '<div class="progress-bar progress-bar-{0}" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div>' +
            '</div>' +
            '<a href="{3}" target="{4}" data-notify="url"></a>' +
        '</div>' 
    });    
}

// 目前總共有兩個地方需要pop-up dialogs，如下:
// 註冊
showPopupDialog('success', 'Sign up successfully', 'Thanks! Please check your email to activate your account.');
// 忘記密碼
showPopupDialog('success', 'Email is sent out!', 'Please check your email to reset your password.');