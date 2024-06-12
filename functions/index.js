const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.createUserOnEmailConfirmation = functions.https.onRequest((req, res) => {
    const email = req.query.email;
    const username = req.query.username;
    const password = req.query.password;

    if (!email || !username || !password) {
        return res.status(400).send('Missing email, username, or password');
    }

    admin.auth().createUser({
        email: email,
        password: password,
        displayName: username
    })
    .then((userRecord) => {
        // Optionally, you can also store additional user info in your database here
        return res.status(200).send('User created successfully');
    })
    .catch((error) => {
        return res.status(500).send('Error creating user: ' + error);
    });
});
