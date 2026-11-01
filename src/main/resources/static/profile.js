document.addEventListener("DOMContentLoaded", () => {

    const userData = localStorage.getItem("user");

    if (!userData) {
        window.location.href = "/";
        return;
    }

    const user = JSON.parse(userData);

    const userName = document.getElementById("userName");
    const profileName = document.getElementById("profileName");
    const profileEmail = document.getElementById("profileEmail");

    if (userName) {
        userName.textContent = user.name;
    }

    if (profileName) {
        profileName.textContent = user.name;
    }

    if (profileEmail) {
        profileEmail.textContent = user.email;
    }

    const logoutBtn = document.getElementById("logoutBtn");

    if (logoutBtn) {
        logoutBtn.addEventListener("click", () => {

            localStorage.removeItem("user");

            window.location.href = "/";

        });
    }

});