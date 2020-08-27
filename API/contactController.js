// Import contact model
Contact = require('./contactModel');

// Handle index actions GET dest
exports.index = function (req, res) {
    Contact.find({
        dest: req.params.dest
    }).exec(function (err, contacts) {
        if (err) {
            res.json({
                status: "error",
                message: err,
            });
        }
        res.json({
            status: "success",
            message: "Predictions retrieved successfully",
            data: contacts
        });        
    });
        
};

/* Created for tests, commented because it does not maintain (dest, exp) as a primary key
// Handle create contact actions POST
exports.new = function (req, res) {
    var contact = new Contact();
    contact.dest = req.params.dest;
    contact.exp = req.params.exp;
    //contact.predictions = req.body.predictions;
    contact.phone = req.body.phone;
    contact.job = req.body.job;
    // save the contact and check for errors
    contact.save(function (err) {
        if (err)
            res.json(err);
        res.json({
            message: 'New prediction created!',
            data: contact
        });
    });
};
*/

// Handle view contact info GET dest + exp
exports.view = function (req, res) {
    Contact.find({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, contact) {
        if (err)
            res.send(err);
        res.json({
            message: 'Prediction details loading..',
            data: contact
        });
    });
};

// Handle update contact info PUT
exports.update = function (req, res) {
    Contact.find({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, contact) {
        if (err) {
            res.send(err);
        } 
        var ncontact = new Contact();
        ncontact.dest = req.params.dest;
        ncontact.exp = req.params.exp;
        //ncontact.predictions = req.body.predictions;
        if (contact[0]) {
            ncontact.phone = contact[0].toObject().phone;
            ncontact.job = contact[0].toObject().job;
        }
        if (req.body.phone) {
            ncontact.phone = req.body.phone;
        }
        if (req.body.job) {
            ncontact.job = req.body.job;
        }

        Contact.remove({
            dest: req.params.dest,
            exp: req.params.exp
        }, function (err, contact) {});

        // save the contact and check for errors
        
        ncontact.save(function (err) {
            if (err)
                res.json(err);
            res.json({
                message: 'Prediction Info updated',
                data: ncontact
            });
        });
    });
};

// Handle delete contact DEL
exports.delete = function (req, res) {
    Contact.remove({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, contact) {
        if (err)
            res.send(err); res.json({
                status: "success",
                message: 'Prediction deleted'
            });
    });
};