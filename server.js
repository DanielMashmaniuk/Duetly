const express = require('express');
const bodyParser = require('body-parser');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const mongoose = require('mongoose');

const app = express();
app.use(bodyParser.json());

mongoose.connect('mongodb+srv://mashmaniukDaniel:<mashmaniuk093548>@cluster0.meamzzg.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0');

const userSchema = new mongoose.Schema({
    email: String,
    password: String,
    verificationCode: String,
    isVerified: { type: Boolean, default: false }
});

const User = mongoose.model('User', userSchema);

app.post('/register', async (req, res) => {
    const { email, password } = req.body;
    const verificationCode = crypto.randomBytes(3).toString('hex'); // 6-значний код

    const newUser = new User({ email, password, verificationCode });
    await newUser.save();

    const transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: 'your-email@gmail.com',
            pass: 'your-email-password'
        }
    });

    const mailOptions = {
        from: 'your-email@gmail.com',
        to: email,
        subject: 'Verification Code',
        text: `Your verification code is: ${verificationCode}`
    };

    transporter.sendMail(mailOptions, (error, info) => {
        if (error) {
            return res.status(500).send(error.toString());
        }
        res.status(200).send('Verification email sent.');
    });
});

app.post('/verify', async (req, res) => {
    const { email, verificationCode } = req.body;

    const user = await User.findOne({ email });

    if (!user) {
        return res.status(400).send('User not found');
    }

    if (user.verificationCode === verificationCode) {
        user.isVerified = true;
        user.verificationCode = null; // Видаляємо код після верифікації
        await user.save();
        res.status(200).send('User verified successfully');
    } else {
        res.status(400).send('Invalid verification code');
    }
});

app.listen(3000, () => {
    console.log('Server started on port 3000');
});
