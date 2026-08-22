// Reserved for future client-side interactions. No behavior yet — server-side
// validation is the source of truth.

(function () {
    var loadMoreButton = document.getElementById('load-more-expenses');
    if (loadMoreButton) {
        var page = 2;
        loadMoreButton.addEventListener('click', function () {
            var tbody = document.querySelector('.recent-table tbody');
            if (!tbody) {
                return;
            }
            fetch('/profile/expenses?page=' + page)
                .then(function (response) {
                    return response.text();
                })
                .then(function (html) {
                    if (!html || !html.trim()) {
                        loadMoreButton.disabled = true;
                        return;
                    }
                    tbody.insertAdjacentHTML('beforeend', html);
                    page += 1;
                })
                .catch(function () {
                    loadMoreButton.disabled = true;
                });
        });
    }
})();
