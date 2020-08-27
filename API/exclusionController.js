// Import exclusion model
Exclusion = require('./exclusionModel');

// Handle view exclusion info
exports.view = function (req, res) {
    Exclusion.find({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, exclusion) {
        if (err)
            res.send(err);
        res.json({
            message: 'Exclusion details loading..',
            data: exclusion
        });
    });
};
// Handle update exclusion info BUG
exports.update = function (req, res) {
    Exclusion.find({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, exclusion) {
        if (err) {
            res.send(err);
        } 
        var nexclusion = new Exclusion();
        nexclusion.dest = req.params.dest;
        nexclusion.exp = req.params.exp;
        if (exclusion[0]) {
            nexclusion.phones = exclusion[0].toObject().phones;
            nexclusion.jobs = exclusion[0].toObject().jobs;
        }
        if (req.body.phone && !nexclusion.phones.includes(req.body.phone)) 
            nexclusion.phones.push(req.body.phone);
        
        if (req.body.job && !nexclusion.jobs.includes(req.body.job))
            nexclusion.jobs.push(req.body.job);

        Exclusion.remove({
            dest: req.params.dest,
            exp: req.params.exp
        }, function (err, exclusion) {});

        // save the exclusion and check for errors
        
        nexclusion.save(function (err) {
            if (err)
                res.json(err);
            res.json({
                message: 'Exclusion Info updated',
                data: nexclusion
            });
        });
    });
};
// Handle delete exclusion
exports.delete = function (req, res) {
    Exclusion.remove({
        dest: req.params.dest,
        exp: req.params.exp
    }, function (err, exclusion) {
        if (err)
            res.send(err); res.json({
                status: "success",
                message: 'Exclusion deleted'
            });
    });
};