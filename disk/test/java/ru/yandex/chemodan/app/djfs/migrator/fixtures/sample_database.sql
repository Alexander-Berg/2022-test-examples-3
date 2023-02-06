--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.9
-- Dumped by pg_dump version 9.6.9

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: folders; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', NULL, '\x9a00859f8e2f01ddb930d42764e61bdbcc42b609b2012a154ed83538f8bd8c5f', '', true, 0, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b4aa76d6-6756-11e9-9b3f-6531499ed0a2', 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', '\xd0cf3911ca0d430b5c7a2641ba6b843df99ac4c96c70d504a8d77fd551f3b226', 'disk', true, 1556195770740153, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b4ac1d9c-6756-11e9-9b3f-6531499ed0a2', 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', '\xac85dd5220dba44321ca7ae1be0c2ede59f31479947407b6f1584f5ab1879d1c', 'trash', true, 1556195770751045, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b4ad11e8-6756-11e9-9b3f-6531499ed0a2', 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', '\xd46ad9b4ded6bf6216be3bca0de45ca986bb8a4b1f48280512e2327b26d1254b', 'hidden', true, 1556195770757352, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, NULL, NULL, NULL, '2018-11-16 15:36:10+03', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b50f3864-6756-11e9-9b3f-6531499ed0a2', 'b4aa76d6-6756-11e9-9b3f-6531499ed0a2', '\xb1f54d8452d1c19b9777daa68c4b79e646156cf4896c5eba801d6bc4df3b73dc', 'filesystem test folder', true, 1556195771398924, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b5134120-6756-11e9-9b3f-6531499ed0a2', 'b50f3864-6756-11e9-9b3f-6531499ed0a2', '\xbe1f2f4d95f064fe360910bfb67ceb1daf3f3300963dd08a3b0086b720a89fd1', 'inner folder', true, 1556195771425483, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b51728da-6756-11e9-9b3f-6531499ed0a2', 'b5134120-6756-11e9-9b3f-6531499ed0a2', '\xd7bb4fb65d00a4fbfc6a2279b03955f1a5b5c56ac7b68e52bcbbaadf8c1aa87a', 'subinner folder', true, 1556195771450528, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', '2019-04-25 15:36:11+03', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO disk.folders (uid, fid, parent_fid, id, name, visible, version, public, public_hash, symlink, short_url, blocked, published, files_count, folders_count, folder_type, date_removed, date_uploaded, date_created, date_modified, date_hidden_data, path_before_remove, modify_uid, download_counter, folder_url, custom_properties, custom_setprop_fields, yarovaya_mark) VALUES (222222, 'b54bf240-6756-11e9-9b3f-6531499ed0a2', 'b4ad11e8-6756-11e9-9b3f-6531499ed0a2', '\xb4a97a331402ac55cdd62fc6b61748664a19aaee1b2e2baa7b5cbadd7564d4f5', 'filesystem test folder copied', true, 1556195771798322, false, NULL, NULL, NULL, false, false, 0, 0, NULL, NULL, NULL, NULL, NULL, '2018-11-16 15:36:11+03', NULL, NULL, NULL, NULL, NULL, '{"dtime": 1556195771}', NULL);


--
-- Data for Name: storage_files; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.storage_files (storage_id, stid, digest_stid, preview_stid, date_origin, full_path, size, md5_sum, sha256_sum, av_scan_status, video_data, width, height, angle)
VALUES
       ('7e5a6b79-7ee6-adac-a122-7ef98d20991a', '1000003.yadisk:89031628.249690056312488962060095667221', '1000005.yadisk:89031628.3983296384177350807526090116783', NULL, '2019-04-25 15:36:11.478865+03', NULL, 10000, '83e5cd52-e94e-3a41-0541-57a6e33226f7', '\x4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865', 'CLEAN', NULL, NULL, NULL, NULL),
       ('a62729bb-c401-3224-8d4b-b0cae8e0ff5e', '100000.yadisk:128280859.945200984434634847', '100000.yadisk:128280859.17656143989754094032', '100000.yadisk:128280859.17309189262960253803', '2019-05-14 19:05:54.409478+03', NULL, 4168404, 'f5073969-9470-e1d0-3888-cc005bba6bbf', '\x477231ccd4690948ed838937f44e737df46493379097c05944f4cd250da42f00', 'CLEAN', NULL, NULL, NULL, NULL);


--
-- Data for Name: storage_files; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.duplicated_storage_files (storage_id, stid)
VALUES
       ('7e5a6b79-7ee6-adac-a122-7ef98d20991a', '1000009.yadisk:89031628.249690056312488962060095667221');


--
-- Data for Name: files; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.files (uid, fid, parent_fid, storage_id, id, name, visible, version, public, public_hash, symlink, short_url, download_counter, folder_url, blocked, source_uid, published, date_created, date_uploaded, date_modified, date_removed, date_hidden_data, path_before_remove, modify_uid, media_type, mime_type, source, custom_properties, custom_setprop_fields, live_photo_hid, date_exif, is_live_photo, yarovaya_mark, ext_aesthetics, photoslice_album_type, albums_exclusions, area) VALUES (222222, 'd3fd616c-4634-11e9-916b-dd2f9d476538', 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', '7e5a6b79-7ee6-adac-a122-7ef98d20991a', E'\\xA0F078A73387D4A04B48FD27C40B68B1348BCFBDB3CD601F90F7B357D0C5D169', '1.jpg', true, 1552552833113007, false, null, null, null, null, null, false, null, false, '2019-03-14 08:40:31.000000', '2019-03-14 08:40:31.000000', '2019-03-14 08:40:31.000000', null, null, null, null, 'image', 'application/x-www-form-urlencoded', 'disk', null, '{"source_platform": "mpfs"}', null, null, true, null, null, null, null, null);
INSERT INTO disk.files (uid, fid, parent_fid, storage_id, id, name, visible, version, public, public_hash, symlink, short_url, download_counter, folder_url, blocked, source_uid, published, date_created, date_uploaded, date_modified, date_removed, date_hidden_data, path_before_remove, modify_uid, media_type, mime_type, source, custom_properties, custom_setprop_fields, live_photo_hid, date_exif, is_live_photo, yarovaya_mark, ext_aesthetics, photoslice_album_type, albums_exclusions, area) VALUES (222222, 'a087fb8c-4637-11e9-916b-dd2f9d476538', 'b4a45cb0-6756-11e9-9b3f-6531499ed0a2', '7e5a6b79-7ee6-adac-a122-7ef98d20991a', E'\\x28D2C8C3428AE453B3661DF29A535A14774397E74836291AF3088995F0EFBB3D', '1.jpg:1552554033.69', true, 1552554034147894, false, null, null, null, null, null, false, null, false, '2019-03-14 08:40:31.000000', '2019-03-14 08:40:31.000000', '2019-03-14 08:40:31.000000', null, '2018-10-05 09:00:34.000000', null, null, 'image', 'application/x-www-form-urlencoded', 'disk', null, '{"id": "/disk/Фотокамера/1.jpg", "source_platform": "mpfs"}', null, null, null, null, null, null, null, null);
INSERT INTO disk.files (uid, fid, parent_fid, storage_id, id, name, visible, version, public, public_hash, symlink, short_url, download_counter, folder_url, blocked, source_uid, published, date_created, date_uploaded, date_modified, date_removed, date_hidden_data, path_before_remove, modify_uid, media_type, mime_type, source, custom_properties, custom_setprop_fields, live_photo_hid, date_exif, is_live_photo, yarovaya_mark, ext_aesthetics, photoslice_album_type, albums_exclusions, area) VALUES (222222, '70fcc212-9db7-11e9-a1ac-17ded6e93480', 'b54bf240-6756-11e9-9b3f-6531499ed0a2', 'a62729bb-c401-3224-8d4b-b0cae8e0ff5e', E'\\x2A07795BC75BCA46105140D2FA88BA62A1A1E3EF191948AD123132CAA86471BD', '2019-06-04 13-50-08.PNG', true, 1562174681233778, false, null, null, null, null, null, false, null, false, '2019-07-03 17:24:41.000000', '2019-07-03 17:24:41.000000', '2019-07-03 17:24:41.000000', null, null, null, null, 'image', 'image/png', 'photounlim', null, '{"screenshot": "1", "source_platform": "andr"}', null, '2019-06-04 10:50:09.000000', null, null, null, 'screenshots', null, null);

--
-- Data for Name: additional_file_links; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.additional_file_links(type, uid, main_file_fid, additional_file_fid) VALUES ('live_video', 222222, 'd3fd616c-4634-11e9-916b-dd2f9d476538', '70fcc212-9db7-11e9-a1ac-17ded6e93480');

--
-- Data for Name: albums; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.albums (id, uid, title, cover_id, cover_offset_y, description, public_key, public_url, short_url, is_public, is_blocked, block_reason, flags, layout, date_created, date_modified, social_cover_stid, fotki_album_id) VALUES (E'\\x5DCA92A8E4D70E34D0F03C9D', 222222, 'Пример', E'\\x5DCA92A8E4D70E34D0F03C96', null, null, 'y3xyxkfgnJT62lfKXy6AZYMTZOh40EdjrsbBCzGzkq8YiD8juUABvUWOZj87DNAKq/J6bpmRyOJonT3VoXnDag==', 'https://disk.dst.yandex.ru/albums/public/?key=y3xyxkfgnJT62lfKXy6AZYMTZOh40EdjrsbBCzGzkq8YiD8juUABvUWOZj87DNAKq%2FJ6bpmRyOJonT3VoXnDag%3D%3D', 'https://yadi.sk/a/YvmY9JePrLK0hA', true, false, null, null, 'rows', '2019-11-12 11:08:24.000000', '2019-11-12 11:08:46.000000', null, null);
INSERT INTO disk.albums (id, uid, title, cover_id, cover_offset_y, description, public_key, public_url, short_url, is_public, is_blocked, block_reason, flags, layout, date_created, date_modified, social_cover_stid, fotki_album_id) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0FE', 222222, 'Другой пример', E'\\x5DC980CAE4D70EA3CBA4F0DC', null, null, 'QaKjoMpai5wyOsFWxsT8NSSrnakMLtb9lOvQUhVFRHt9Z5S3oosdwaQLC7cWRmHCq/J6bpmRyOJonT3VoXnDag==', 'https://disk.dst.yandex.ru/albums/public/?key=QaKjoMpai5wyOsFWxsT8NSSrnakMLtb9lOvQUhVFRHt9Z5S3oosdwaQLC7cWRmHCq%2FJ6bpmRyOJonT3VoXnDag%3D%3D', 'https://yadi.sk/a/1fv_rjccQ7GcQA', true, false, null, null, 'rows', '2019-11-11 15:39:54.000000', '2019-11-11 15:40:33.000000', null, null);
INSERT INTO disk.albums (id, uid, title, cover_id, cover_offset_y, description, public_key, public_url, short_url, is_public, is_blocked, block_reason, flags, layout, date_created, date_modified, social_cover_stid, fotki_album_id, album_type) VALUES (E'\\x5555fa2dde33ec5a2ca261cb', 222222, 'Faces album example', E'\\x5555fd04de33ec42b221b6c6', null, null, 'HQLqIhGbeoIxr5L73iSq5tvO00m8hj8hkaTd21CP8xcx7XiaJjIQ6K2RPx/5+4gvq/J6bpmRyOJonT3VoXnDag==', 'https://disk-test.disk.yandex.ru/albums/public/?key=HQLqIhGbeoIxr5L73iSq5tvO00m8hj8hkaTd21CP8xcx7XiaJjIQ6K2RPx%2F5%2B4gvq%2FJ6bpmRyOJonT3VoXnDag%3D%3D', 'https://front.tst.clk.yandex.net/a/LOdzK0AbVYDm', false, false, null, null, 'rows', '2019-11-11 15:39:54.000000', '2019-11-11 15:40:33.000000', null, null, 'faces');

--
-- Data for Name: album_items; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0DC', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 1, 'c45a8126473b5373fca077326ad626e2a6eb4f222237271cb7ad9feb2bc949e9', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0E7', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 12, 'cba92caebcbab5b57228627e4de738357ea8b32007d282e9f7f77c72c91f8f3c', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0FC', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 33, 'd3ae3a56350c51d8e66444e6e4279e5a3dc122e8d71c3b7619a0a6435aa48af8', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0EC', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 17, 'd9832b722715f87f464c6a377aa6b5a14f6de8264d64c9ada0d37a2686b12713', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0E5', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 10, 'e0e48d1b550138e7f63181e15cab2c5ff40b6e1008fa5f370fb37888a0dc04d6', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0DE', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 3, 'e3b5282fbb63af0e922d8bd97b5a1b261c4aabdbe2081e4fb4a7d0e4b0d1029d', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0F1', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 22, 'ea339ef32560c21a01cece5111a49ed0644688274c085dfbd894097f6ebd5311', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0FB', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 32, 'f89c48ddc2bbfb603f0829f0531bf42f458c115f2f4aeb668def4c6a4edabd27', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DC980CAE4D70EA3CBA4F0F8', 222222, E'\\x5DC980CAE4D70EA3CBA4F0FE', null, null, 29, 'f906d0c638f1eaf06ea5cb846691c5ca5e473df244d92544570ca3822ed10b9c', 'resource');

INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C9B', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 6, '1f5207f04ae924939483e024c3aa0a2d6d1cedcfb6d378a69f6e029a4d1c31b7', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C96', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 1, '22c2a726c3d167e010f5527639990e776e38803eabd51f18d58a765bd368d1c2', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C97', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 2, '2881b6759a1de70ffd4f3b7b2fd3360f0ada3c92ae3018e87c2ce3d4bfb6a9d1', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C99', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 4, '2ead82c02bb4eb33a5aab7730717b6f97f504653e505e9e877efec6bd9b18a17', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C98', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 3, '5a7277e2ca677a5eb12d9a6f624bba5a680a140295b76907480eb2489a6e14b9', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C9C', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 7, '7b03b546ac18cecf33e83f264bf91177987447492913d188eaba7c1e5376441f', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5DCA92A8E4D70E34D0F03C9A', 222222, E'\\x5DCA92A8E4D70E34D0F03C9D', null, null, 5, '9320e9108da8ea21797305b6fd1a949c248741bad65679adfe63125b36cfd0a9', 'resource');
INSERT INTO disk.album_items (id, uid, album_id, description, group_id, order_index, obj_id, obj_type) VALUES (E'\\x5555fd04de33ec42b221b6c6', 222222, E'\\x5555fa2dde33ec5a2ca261cb', null, null, 94, '8c6e663580abf91c8dec5fa76895443633c266ee8917b7172af7bd311bc3d66d', 'resource');

--
-- Data for Name: album_deltas; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.album_deltas (id, uid, revision, changes) VALUES ('77de3de8-c5ff-4d56-83dc-57e7aa061f9b', 222222, 100500, '{"delta_id":"addingnewevent","changes":[{"record_id":"20.05.2016","collection_id":"Friday","change_type":"insert","changes":[{"field_id":"event_type","change_type":"set","value":{"type":"string","string":"Meeting"}},{"field_id":"hour","change_type":"set","value":{"type":"datetime","datetime":"2016-05-20T15:00:00.118000+00:00"}},{"field_id":"duration","change_type":"set","value":{"type":"string","string":"120min"}}]}]}'::jsonb);

--
-- Data for Name: albums_info; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.albums_info (uid, revision) VALUES (222222, 100500);

--
-- Data for Name: album_face_clusters; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.album_face_clusters (uid, cluster_id, album_id) VALUES (222222, '4017822520_1_1', E'\\x5555fa2dde33ec5a2ca261cb');

--
-- Data for Name: async_tasks_data; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--



--
-- Data for Name: changelog; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9ba3c74a24774b4e6e6', 222222, '/disk', 'dir', 'new', 1556195770740153, '{"fid": null, "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:10.744289+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6e7', 222222, '/disk/filesystem test folder', 'dir', 'new', 1556195771398924, '{"fid": "b1f54d8452d1c19b9777daa68c4b79e646156cf4896c5eba801d6bc4df3b73dc", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.404173+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6e8', 222222, '/disk/filesystem test folder/inner folder', 'dir', 'new', 1556195771425483, '{"fid": "be1f2f4d95f064fe360910bfb67ceb1daf3f3300963dd08a3b0086b720a89fd1", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.430795+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6e9', 222222, '/disk/filesystem test folder/inner folder/subinner folder', 'dir', 'new', 1556195771450528, '{"fid": "d7bb4fb65d00a4fbfc6a2279b03955f1a5b5c56ac7b68e52bcbbaadf8c1aa87a", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.456271+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6ea', 222222, '/disk/filesystem test file', 'file', 'new', 1556195771476745, '{"fid": "8810a2a10dbacbd0f1d3892edf822466e260fc807b4dd42030d489869d3746b6", "md5": "83e5cd52e94e3a41054157a6e33226f7", "size": 10000, "drweb": 4, "etime": 0, "mtime": 1556195771, "public": 0, "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865", "visible": 1, "mimetype": "text/plain", "media_type": "text", "has_preview": false}', NULL, NULL, '2019-04-25 15:36:11.48527+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6eb', 222222, '/disk/filesystem test folder/inner file', 'file', 'new', 1556195771512120, '{"fid": "4ebafebd5a225dc8dc9a544f4e2ef9808af6c2093e756b2dae3284dd7f407e52", "md5": "83e5cd52e94e3a41054157a6e33226f7", "size": 10000, "drweb": 4, "etime": 0, "mtime": 1556195771, "public": 0, "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865", "visible": 1, "mimetype": "text/plain", "media_type": "text", "has_preview": false}', NULL, NULL, '2019-04-25 15:36:11.520764+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6ec', 222222, '/disk/filesystem test folder copied', 'dir', 'new', 1556195771585798, '{"fid": "53574d1f1b62dd551d328ba31458f415c8031bbbfa74d6051c14761cc2a642f3", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.590795+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6ed', 222222, '/disk/filesystem test folder copied/inner file', 'file', 'new', 1556195771616950, '{"fid": "be1b839b766eefc5a93ae3ec6589e811d1c0823548af52a58a52889ee22bc1d8", "md5": "83e5cd52e94e3a41054157a6e33226f7", "size": 10000, "drweb": 1, "etime": 0, "mtime": 1556195771, "public": 0, "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865", "visible": 1, "mimetype": "text/plain", "media_type": "text", "has_preview": false}', NULL, NULL, '2019-04-25 15:36:11.625603+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6ee', 222222, '/disk/filesystem test folder copied/inner folder', 'dir', 'new', 1556195771649717, '{"fid": "5e6305112f73a8517d3c42db15ee324fa6a5df77529008ad84c6de56473f1aea", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.654845+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6ef', 222222, '/disk/filesystem test folder copied/inner folder/subinner folder', 'dir', 'new', 1556195771678167, '{"fid": "1281e6db0e9ebd05ab7ca094b55a04c0c471d7157db783bebdf51194d8ae8c68", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.684033+03', NULL, NULL);
INSERT INTO disk.changelog (id, uid, path, type, op, version, zdata, gid, group_path, dtime, shared, rights) VALUES ('\x5cc1a9bb3c74a24774b4e6f0', 222222, '/disk/filesystem test folder copied', 'dir', 'deleted', 1556195771829687, '{"fid": "53574d1f1b62dd551d328ba31458f415c8031bbbfa74d6051c14761cc2a642f3", "public": 0, "visible": 1}', NULL, NULL, '2019-04-25 15:36:11.830176+03', NULL, NULL);


--
-- Data for Name: deletion_log; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.deletion_log (uid, file_id, storage_id, deletion_log_revision, is_live_photo) VALUES (222222, E'\\x9E48686CD31005B25CB463D247E5E26FA0C39E5C7B9A240DD6C8C7E8E0C0274E', 'eb0ce42b-48d7-2ff7-bfd3-e6ebc15e79fb', '2019-11-07 07:44:45.041502', false);

--
-- Data for Name: disk_info; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.disk_info (id, uid, parent, version, path, type, data) VALUES ('e461677d-bf52-09a8-c4ff-b5634ca095c8', 222222, NULL, 0, '/', 'dir', '{}');
INSERT INTO disk.disk_info (id, uid, parent, version, path, type, data) VALUES ('e147ab37-a961-8125-448e-89cbf6a66dbc', 222222, 'e461677d-bf52-09a8-c4ff-b5634ca095c8', 1556195770764966, '/trash_size', 'file', '0');
INSERT INTO disk.disk_info (id, uid, parent, version, path, type, data) VALUES ('9b827669-388f-5c29-aec1-c3e072230d50', 222222, 'e461677d-bf52-09a8-c4ff-b5634ca095c8', 1556195770768114, '/limit', 'file', '10737418240');
INSERT INTO disk.disk_info (id, uid, parent, version, path, type, data) VALUES ('02a7cfbf-eae6-2674-495c-e345dc06ecfe', 222222, 'e461677d-bf52-09a8-c4ff-b5634ca095c8', 1556195770761887, '/total_size', 'file', '20000');


--
-- Data for Name: filesystem_locks; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.filesystem_locks (id, uid, path, dtime, data) VALUES ('1370162c-5887-6eb2-2f61-97d6d6423444', 222222, '/disk/home/documents/generated/generated_docs/49', '2019-11-12 14:25:09.459342', '{"oid": "fc27e281592d0aaedc1a9c41e3d75b7f1ff298ed68591bf69db07c758892fef3", "op_type": "copy_resource"}');
INSERT INTO disk.filesystem_locks (id, uid, path, dtime, data) VALUES ('8bf481f7-11e9-d5b8-b5e3-8545fa86b088', 222222, '/disk/home/documents/generated/generated_docs/48', '2019-11-12 14:25:22.168969', '{"oid": "7f37005baf3b531ae011dcfa256a1bdd2600c919b256d2ec8e676efb6b287462", "op_type": "copy_resource"}');


--
-- Data for Name: groups; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--



--
-- Data for Name: group_invites; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--



--
-- Data for Name: group_links; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--



--
-- Data for Name: last_files_cache; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--



--
-- Data for Name: link_data; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.link_data (id, uid, path, version, user_ip, public_uid, target, type, parent, date_created, date_modified, date_deleted, file_id, resource_id_uid) VALUES ('e461677d-bf52-09a8-c4ff-b5634ca095c8', 222222, '/', NULL, NULL, NULL, NULL, 'dir', NULL, NULL, NULL, NULL, NULL, NULL);


--
-- Data for Name: misc_data; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.misc_data(id, uid, path, type, parent, version, zdata, file_id, file_id_zipped, hid, mimetype, mediatype, visible, "size", file_stid, digest_stid, preview_stid, date_modified, date_uploaded, date_exif) VALUES ('9bf90982-408c-1841-873f-d192640f323a', 222222, '/settings', 'dir', '4755125e-992f-023a-c883-640329de502d', '1433248382540427', '\x7801ab562a4e2d2928ca2f50b2aa564acbcc498dcf4c89afca2c28484d51b22a292a4dadad0500ed0a0d71', '\x62626133613834373630376635343438656231343233323362656630643336373666313564613231353932663536363263363662663365626337653538313538', 't', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO disk.misc_data(id, uid, path, type, parent, version, zdata, file_id, file_id_zipped, hid, mimetype, mediatype, visible, "size", file_stid, digest_stid, preview_stid, date_modified, date_uploaded, date_exif) VALUES ('c9a64319-70de-f96b-83fa-3054aae6ad9b', 222222, '/settings/Favorites.arch', 'file', '9bf90982-408c-1841-873f-d192640f323a', '1433248383174468', '\x78010dcc310ec3200c40d1bb78ce80310693db181bd40ca85292aa43d4bb97f10fff3d30fbadb03f70bd3fa775d8611e97c106d74b23e7d535a2072aa2c570484d380a491b8a46d2091b97a8cd33a6ce685dc483061a9a71a04b2ab22cbb8fb9684c443109096d309d974d3a2c7390c14e8658138bc59c1b59ac8d6359af9fdfded6fbfbfd015b9d2f3b', '\x66353333326138353533366638396431663662343634653430306161643831343332363064383862646161336162646534366638633339366538636237303232', null, '67554ca5-7b41-b2d0-3c0d-76685ba7fa9a', 'application/octet-stream', 'data', 't', '1178', '55583.yadisk:123.123', '54967.yadisk:123.123', null, '2015-06-02 15:33:03', '2015-06-02 15:33:03', null);
INSERT INTO disk.misc_data(id, uid, path, type, parent, version, zdata, file_id, file_id_zipped, hid, mimetype, mediatype, visible, "size", file_stid, digest_stid, preview_stid, date_modified, date_uploaded, date_exif) VALUES ('4755125e-992f-023a-c883-640329de502d', 222222, '/', 'dir', null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

--
-- Data for Name: operations; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.operations (id, uid, ctime, dtime, mtime, state, version, type, subtype, md5, uniq_id, data, ycrid, lock_id, date_lock_acquired) VALUES ('e6d86e4a398eef1a7b8a2be5bb3a3b336c9f213a3b1586e283fc10ed8cfa9c79', 222222, '2019-11-07 08:11:10.000000', '2019-11-07 08:11:21.959186', '2019-11-07 08:11:21.000000', 6, 1573114281958931, 'trash', 'append', null, 'baa60ccf-40dc-c4ca-ec46-a45877011c68', '{"path": "222222:/disk/home/documents/generated/17.txt", "stages": {}, "file_id": "997e782b01a7520c2e4d9b827333a264f2232341f2774bb0fdd90f8eb68eacc8", "callback": "", "filedata": {}, "at_version": 1573114266337821, "connection_id": "2222221573114249185", "affected_resource": "/trash/17.txt", "source_resource_id": "222222:997e782b01a7520c2e4d9b827333a264f2232341f2774bb0fdd90f8eb68eacc8"}', null, null, null);
INSERT INTO disk.operations (id, uid, ctime, dtime, mtime, state, version, type, subtype, md5, uniq_id, data, ycrid, lock_id, date_lock_acquired) VALUES ('878558b83b17158011fafabb7c6eba939c2db453ea5e0fa8e5c3265679502a20', 222222, '2019-11-07 07:44:14.000000', '2019-11-07 07:44:48.251579', '2019-11-07 07:44:48.000000', 6, 1573112688251433, 'trash', 'append', null, '3f56cddb-0f0f-87c7-c4e7-5be8736ed609', '{"path": "222222:/disk/home/documents/1233.odg", "stages": {}, "file_id": "9e48686cd31005b25cb463d247e5e26fa0c39e5c7b9a240dd6c8c7e8e0c0274e", "callback": "", "filedata": {}, "at_version": 1573112654167976, "connection_id": "2222221573112640571", "affected_resource": "/trash/1233.odg", "source_resource_id": "222222:9e48686cd31005b25cb463d247e5e26fa0c39e5c7b9a240dd6c8c7e8e0c0274e"}', null, null, null);
INSERT INTO disk.operations (id, uid, ctime, dtime, mtime, state, version, type, subtype, md5, uniq_id, data, ycrid, lock_id, date_lock_acquired) VALUES ('884bacd7f61cfa713af53dcb78dbe3f17a4aa104f045ba712c0087958e3f2e9d', 222222, '2019-11-07 07:44:14.000000', '2019-11-07 07:44:48.444801', '2019-11-07 07:44:48.000000', 6, 1573112688444620, 'trash', 'append', null, 'c62bacdc-b0e4-d6d3-5760-0aab796d2b2e', '{"path": "222222:/disk/home/documents/2018.09.10.pdf", "stages": {}, "file_id": "571efd53067482aa4352e1b208a7232d44ffad4a5c4176b03a1ddc15c1e99fac", "callback": "", "filedata": {}, "at_version": 1573112654167976, "connection_id": "2222221573112640571", "affected_resource": "/trash/2018.09.10.pdf", "source_resource_id": "222222:571efd53067482aa4352e1b208a7232d44ffad4a5c4176b03a1ddc15c1e99fac"}', null, null, null);
INSERT INTO disk.operations (id, uid, ctime, dtime, mtime, state, version, type, subtype, md5, uniq_id, data, ycrid, lock_id, date_lock_acquired) VALUES ('876b4203d2cf20f9ef21658990c0fdb6dc603ff5c0fad52d679e2d562fa172da', 222222, '2019-11-07 08:11:10.000000', '2019-11-07 08:11:22.365534', '2019-11-07 08:11:22.000000', 6, 1573114282365278, 'trash', 'append', null, '1c2f3e62-336a-8475-2e1f-35796a774242', '{"path": "222222:/disk/home/documents/generated/13.txt", "stages": {}, "file_id": "e3d5d91e52458eeb559fabafa2008fda14ceace4a9e16b156ced28653fa99bc4", "callback": "", "filedata": {}, "at_version": 1573114266337821, "connection_id": "2222221573114249185", "affected_resource": "/trash/13.txt", "source_resource_id": "222222:e3d5d91e52458eeb559fabafa2008fda14ceace4a9e16b156ced28653fa99bc4"}', null, null, null);
INSERT INTO disk.operations (id, uid, ctime, dtime, mtime, state, version, type, subtype, md5, uniq_id, data, ycrid, lock_id, date_lock_acquired) VALUES ('c1760e5eb776ce94afb6c240d081f1fc9ba5a824698511a9d5d43c117cff8b9e', 222222, '2019-11-07 08:11:11.000000', '2019-11-07 08:11:36.960290', '2019-11-07 08:11:36.000000', 6, 1573114296960008, 'trash', 'append', null, '6aa729b2-87a1-8368-2f81-2b0c6f894758', '{"path": "222222:/disk/home/documents/generated/20.txt", "stages": {}, "file_id": "8bbde0cd43bd97162db99aa30c4a104b22216a197a075cd07e7c173b13fb3b25", "callback": "", "filedata": {}, "at_version": 1573114266337821, "connection_id": "2222221573114249185", "affected_resource": "/trash/20.txt", "source_resource_id": "222222:8bbde0cd43bd97162db99aa30c4a104b22216a197a075cd07e7c173b13fb3b25"}', null, null, null);

--
-- Data for Name: source_ids; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.source_ids (uid, storage_id, source_id, is_live_photo) VALUES (222222, '7e5a6b79-7ee6-adac-a122-7ef98d20991a', '123asd13', true);

--
-- Data for Name: user_index; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.user_index (uid, version, blocked, deleted, user_type, reg_time, locale, shard_key, b2b_key, pdd, yateam_uid, collections, is_mailish, hancom_enabled, unlimited_autouploading_enabled, lock_key, lock_timestamp, last_quick_move_version, is_reindexed_for_quick_move) VALUES (222222, 1556195771829687, false, NULL, 'standart', '2019-04-25 15:36:10+03', 'ru', 39991, NULL, NULL, NULL, '{user_data,disk_info,trash,hidden_data,link_data}', false, false, false, NULL, NULL, NULL, NULL);


--
-- Data for Name: user_activity_info; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.user_activity_info (uid, platform_type, first_activity, last_activity) VALUES (222222, 'ios', '2019-01-01', '2019-01-01'), (222222, 'android', '2019-01-01', '2019-01-01');


--
-- Data for Name: version_links; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.version_links (id, uid, file_id, disk_path, disk_path_hash, date_created)
VALUES ('28d0c15f-06b6-49af-b596-661ab834caaf', 222222, '\xe8456380856eff702206123737af8683821004aa7f835ff2e83ce628780efc15', NULL, NULL, '2019-05-14 19:05:54.771045+03');
INSERT INTO disk.version_links (id, uid, file_id, disk_path, disk_path_hash, date_created) VALUES ('86e2088a-e300-49c4-ac3f-f096dd1ddd41', 222222, E'\\x268EE2914E6C103B7DB3AE95DD65F57288B4469E1F08122AABB69972B932ACF4', '/disk/zzz2/MediaJava.xml', '4d7a56e0-436a-a033-3bdc-ff19554a4224', '2018-11-26 13:34:40.744845');

--
-- Data for Name: version_data; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.version_data (id, uid, version_link_id, parent_version_id, type, date_created, is_checkpoint, folded_counter, uid_created, platform_created, storage_id, date_exif, record_date_created, date_to_remove)
VALUES ('d61f360d-0b5e-4150-a679-1b680ec39fde', 222222, '28d0c15f-06b6-49af-b596-661ab834caaf', NULL, 'binary', '2019-05-14 19:05:54+03', TRUE, 0, 222222, 'mpfs', 'a62729bb-c401-3224-8d4b-b0cae8e0ff5e', '2019-05-14 22:05:54+03', '2019-05-14 19:05:54.769561+03', '2019-08-12 19:05:54.769426+03');
INSERT INTO disk.version_data (id, uid, version_link_id, parent_version_id, type, date_created, is_checkpoint, folded_counter, uid_created, platform_created, storage_id, date_exif, record_date_created, date_to_remove) VALUES ('191675b9-47ca-47da-bfc8-b17add358e28', 222222, '86e2088a-e300-49c4-ac3f-f096dd1ddd41', 'f539f2c4-28f6-497c-93fd-1f534d8958bf', 'trashed', '2018-11-26 13:34:40.000000', true, 1, 222222, 'web', null, null, '2018-11-26 13:34:40.750608', '2019-02-24 13:34:40.750242');

--
-- Data for Name: last_files_cache; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.last_files_cache(id, uid, gid, owner_uid, file_id, file_date_modified, date_created) VALUES ('6f0eaeb2-196e-40bb-9d53-43f6422232a5', 222222, 'd75f0270-a038-abaa-1951-0b4d20146798', 222222, '\x9acb5b9ddfdbcc933e6dc339e0bda13102e84a068b65677c2c8cbaeef92fb4e5', '2019-12-15 22:57:39+03', '2019-12-15 22:57:45.312097+03');
INSERT INTO disk.last_files_cache(id, uid, gid, owner_uid, file_id, file_date_modified, date_created) VALUES ('abe4a967-46ab-4146-8547-347d8582812c', 222222, 'f3c416f2-59f9-651d-d654-7770174a58ad', 222222, '\x2260bc9f1317ce3e633b47b65238649cb58dffe4d62abb726da5ced5572ece39', '2018-11-14 22:38:13+03', '2018-11-14 22:39:22.453467+03');
INSERT INTO disk.last_files_cache(id, uid, gid, owner_uid, file_id, file_date_modified, date_created) VALUES ('9fb5c770-d99f-40bc-a12b-b90295ab8231', 222222, '8057354d-f818-6604-893f-5c02d122e1b8', 222222, '\x452b7920c15bd896fc3016924c8e8c1afa46f2314806126eb2728bae60703d79', '2019-06-25 12:17:19+03', '2019-06-25 12:17:24.892519+03');

--
-- Data for Name: async_tasks_data; Type: TABLE DATA; Schema: disk; Owner: disk_mpfs
--

INSERT INTO disk.async_tasks_data(id, uid, date_created, data) VALUES ('abe4a967-46ab-4146-8547-347d8582812c', 222222, '2019-06-25 12:17:24.892519+03', 'some data');

--
-- PostgreSQL database dump complete
--
