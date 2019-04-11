var GLOBAL_CONFIG = {
    version: 'v-2.0.26',
    apiHostUrl: getSSOUri()  + '/v2.0',
    webHostUrl: getSSOUri()  + '/web',
    tpHostUrl: getSSOUri().replace('portal-sso', 'portal-technical'),
    platform: getPlatform(window.location.host)
};

function getPlatform(host) {
    if (host.indexOf('iii-cflab.com') >= 0) {
        return "III-cflab";
    } else if (host.indexOf('tw-cflab.co') >= 0) {
        return "tw-cflab";
    } else if (host.indexOf('iii-arfa.com') >= 0) {
        return "III-arfa";
    } else if (host.indexOf('wise-paas.com') >= 0) {
        if (host.indexOf('cn') >= 0) {
            return "Beijing";
        } else if (host.indexOf('ali') >= 0) {
            return "Ali";
        } else {
            return "HongKong";
        }
    } else {
        return "Develop";
    }
}

function getSSOUri() {
    // return window.location.protocol + '//' + window.location.host;
    return  'https://portal-sso.wise-paas.com';
}
